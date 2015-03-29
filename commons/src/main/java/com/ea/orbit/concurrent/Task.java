/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ea.orbit.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Task<T> extends CompletableFuture<T>
{
    private static Void NIL = null;

    /**
     * Creates an already completed task from the given value.
     *
     * @param value the value to be wrapped with a task
     */
    public static <T> Task<T> fromValue(T value)
    {
        final Task<T> t = new Task<T>();
        t.complete(value);
        return t;
    }

    /**
     * Wraps a CompletionStage as a Task or just casts it if it is already a Task.
     *
     * @param stage the stage to be wrapped or casted to Task
     * @return stage cast as Task of a new Task that is dependent on the completion of that stage.
     */
    public static <T> Task<T> from(CompletionStage<T> stage)
    {
        if (stage instanceof Task)
        {
            return (Task<T>) stage;
        }

        final Task<T> t = new Task<T>();
        stage.handle((T v, Throwable ex) -> {
            if (ex != null)
            {
                t.completeExceptionally(ex);
            }
            else
            {
                t.complete(v);
            }
            return null;
        });
        return t;
    }

    /**
     * Wraps a Future as a Task or just casts it if it is already a Task.
     * <p>If future implements CompletionStage CompletionStage.handle will be used.</p>
     * <p>
     * If future is a plain old future, a runnable will executed using a default task pool.
     * This runnable will wait on the future using Future.get(timeout,timeUnit), with a timeout of 5ms.
     * Every time this task times out it will be rescheduled.
     * Starvation of the pool is prevented by the rescheduling behaviour.
     * </p>
     *
     * @param future the future to be wrapped or casted to Task
     * @return future cast as Task of a new Task that is dependent on the completion of that future.
     */
    public static <T> Task<T> fromFuture(Future<T> future)
    {
        if (future instanceof Task)
        {
            return (Task<T>) future;
        }
        if (future instanceof CompletionStage)
        {
            return from((CompletionStage<T>) future);
        }

        final Task<T> t = new Task<T>();

        if (future.isDone())
        {
            try
            {
                t.complete(future.get());
            }
            catch (Throwable ex)
            {
                t.completeExceptionally(ex);
            }
            return t;
        }

        // potentially very expensive
        commonPool.execute(new TaskFutureAdapter(t, future, commonPool, 5, TimeUnit.MILLISECONDS));
        return t;
    }

    private static Executor commonPool = ExecutorUtils.newScalingThreadPool(100);

    @SuppressWarnings("unsafe")
    static class TaskFutureAdapter<T> implements Runnable
    {
        Task<T> task;
        Future<T> future;
        Executor executor;
        long waitTimeout;
        TimeUnit waitTimeoutUnit;

        public TaskFutureAdapter(
                final Task<T> task,
                final Future<T> future,
                final Executor executor,
                long waitTimeout, TimeUnit waitTimeoutUnit)
        {
            this.task = task;
            this.future = future;
            this.executor = executor;
            this.waitTimeout = waitTimeout;
            this.waitTimeoutUnit = waitTimeoutUnit;
        }

        public void run()
        {
            try
            {
                while (!task.isDone())
                {
                    try
                    {
                        task.complete(future.get(waitTimeout, waitTimeoutUnit));
                        return;
                    }
                    catch (TimeoutException ex)
                    {
                        if (future.isDone())
                        {
                            // in this case something completed the future with a timeout exception.
                            try
                            {
                                task.complete(future.get(waitTimeout, waitTimeoutUnit));
                                return;
                            }
                            catch (Throwable tex0)
                            {
                                task.completeExceptionally(tex0);
                            }
                            return;
                        }
                        try
                        {
                            // reschedule
                            // potentially very expensive, might limit request throughput
                            executor.execute(this);
                            return;
                        }
                        catch (RejectedExecutionException rex)
                        {
                            // ignoring and continuing.
                            // might potentially worsen an already starving system.
                            continue;
                        }
                        catch (Throwable tex)
                        {
                            task.completeExceptionally(tex);
                            return;
                        }
                    }
                    catch (Throwable ex)
                    {
                        task.completeExceptionally(ex);
                        return;
                    }
                }
            }
            catch (Throwable ex)
            {
                task.completeExceptionally(ex);
            }
        }
    }


    public static Task<Void> done()
    {
        final Task<Void> task = new Task<Void>();
        task.complete(NIL);
        return task;
    }

    @Override
    public <U> Task<U> thenApply(final Function<? super T, ? extends U> fn)
    {
        return from(super.thenApply(fn));
    }

    @Override
    public Task<Void> thenAccept(final Consumer<? super T> action)
    {
        return from(super.thenAccept(action));
    }

    @Override
    public Task<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action)
    {
        return from(super.whenComplete(action));
    }

    /**
     * Returns a new Task that is executed when this task completes normally.
     * The result of the new Task will be the result of the Supplier passed as parameter.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param supplier the Supplier that will provider the value
     *                 the returned Task
     * @param <U>      the supplier's return type
     * @return the new Task
     */
    public <U> Task<U> thenReturn(final Supplier<U> supplier)
    {
        return from(super.thenApply(x -> supplier.get()));
    }

    public <U> Task<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)
    {
        return Task.from(super.thenCompose(fn));
    }

    public <U> Task<U> thenCompose(Supplier<? extends CompletionStage<U>> fn)
    {
        return Task.from(super.thenCompose((T x) -> fn.get()));
    }

    @Override
    public <U> Task<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn)
    {
        return from(super.handle(fn));
    }

    @Override
    public Task<T> exceptionally(final Function<Throwable, ? extends T> fn)
    {
        return from(super.exceptionally(fn));
    }

    @Override
    public Task<Void> thenRun(final Runnable action)
    {
        return from(super.thenRun(action));
    }

    /**
     * @throws NullPointerException if the array or any of its elements are
     *                              {@code null}
     */
    public static Task<Void> allOf(CompletableFuture<?>... cfs)
    {
        return from(CompletableFuture.allOf(cfs));
    }

    /**
     * @throws NullPointerException if the collection or any of its elements are
     *                              {@code null}
     */
    public static <F extends CompletableFuture, C extends Collection<F>> Task<C> allOf(C cfs)
    {
        return from(CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]))
                .thenApply(x -> cfs));
    }

    /**
     * @throws NullPointerException if the stream or any of its elements are
     *                              {@code null}
     */
    public static <F extends CompletableFuture<?>> Task<List<F>> allOf(Stream<F> cfs)
    {
        final List<F> futureList = cfs.collect(Collectors.toList());
        final CompletableFuture[] futureArray = futureList.toArray(new CompletableFuture[futureList.size()]);
        return from(CompletableFuture.allOf(futureArray).thenApply(x -> futureList));
    }

    /**
     * @throws NullPointerException if the array or any of its elements are
     *                              {@code null}
     */
    public static Task<Object> anyOf(CompletableFuture<?>... cfs)
    {
        return from(CompletableFuture.anyOf(cfs));
    }

    /**
     * @throws NullPointerException if the collection or any of its elements are
     *                              {@code null}
     */
    public static <F extends CompletableFuture> Task<Object> anyOf(Collection<F> cfs)
    {
        return from(CompletableFuture.anyOf(cfs.toArray(new CompletableFuture[cfs.size()])));
    }

    /**
     * @throws NullPointerException if the stream or any of its elements are
     *                              {@code null}
     */
    public static <F extends CompletableFuture<?>> Task<Object> anyOf(Stream<F> cfs)
    {
        return from(CompletableFuture.anyOf((CompletableFuture[])cfs.toArray(size -> new CompletableFuture[size])));
    }

}
