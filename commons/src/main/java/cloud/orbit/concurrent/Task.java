/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Task are CompletableFutures were with a few changes.
 * <p>
 * <ul>
 * <li>complete and completeExceptionally are not publicly accessible.</li>
 * <li>few utility methods (thenRun, thenReturn)</li>
 * <li>TODO: all callbacks are async by default using the current executor</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of the returned object.
 * @see java.util.concurrent.CompletableFuture
 */
public class Task<T> extends CompletableFuture<T>
{
    private static final Void NIL = null;

    private static Executor commonPool = ExecutorUtils.newScalingThreadPool(100);
    private static ScheduledExecutorService schedulerExecutor = new ScheduledThreadPoolExecutor(10, runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("OrbitTaskThread");
        thread.setDaemon(true);
        return thread;
    });

    // TODO: make all callbacks async by default and using the current executor
    // what "current executor' means will have to be defined.
    // the idea is to use a framework supplied executor to serve
    // single point to capture all activity  derived from the execution
    // of one application request.
    // Including logs, stats, exception and timing information.

    // TODO: consider creating a public class CTask = "Completable Task"

    final Executor defaultExecutor;

    public Task()
    {
        TaskContext context = TaskContext.current();
        if (context != null)
        {
            this.defaultExecutor = context.getDefaultExecutor();
        }
        else
        {
            this.defaultExecutor = null;
        }
    }

    /**
     * Creates an already completed task from the given value.
     *
     * @param value the value to be wrapped with a task
     */
    public static <T> Task<T> fromValue(T value)
    {
        final Task<T> t = new Task<>();
        t.internalComplete(value);
        return t;
    }

    public static <T> Task<T> fromException(Throwable ex)
    {
        final Task<T> t = new Task<>();
        t.internalCompleteExceptionally(ex);
        return t;
    }


    protected boolean internalComplete(T value)
    {
        return super.complete(value);
    }

    protected boolean internalCompleteExceptionally(Throwable ex)
    {
        return super.completeExceptionally(ex);
    }

    /**
     * This completableFuture derived method is not available for Tasks.
     */
    @Override
    public boolean complete(T value)
    {
        // TODO: throw an exception
        return super.complete(value);
    }

    /**
     * This completableFuture derived method is not available for Tasks.
     */
    @Override
    public boolean completeExceptionally(Throwable ex)
    {
        // TODO: throw an exception
        return super.completeExceptionally(ex);
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

        final Task<T> t = new Task<>();
        stage.handle((T v, Throwable ex) -> {
            if (ex != null)
            {
                t.internalCompleteExceptionally(ex);
            }
            else
            {
                t.internalComplete(v);
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
    @SuppressWarnings("unchecked")
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

        final Task<T> t = new Task<>();

        if (future.isDone())
        {
            try
            {
                t.internalComplete(future.get());
            }
            catch (Throwable ex)
            {
                t.internalCompleteExceptionally(ex);
            }
            return t;
        }

        // potentially very expensive
        commonPool.execute(new TaskFutureAdapter<>(t, future, commonPool, 5, TimeUnit.MILLISECONDS));
        return t;
    }


    /**
     * Returns a new task that will fail if the original is not completed withing the given timeout.
     * This doesn't modify the original task in any way.
     * <p/>
     * Example:
     * <pre><code>Task&lt;String> result = aTask.failAfter(60, TimeUnit.SECONDS);</code></pre>
     *
     * @param timeout  the time from now
     * @param timeUnit the time unit of the timeout parameter
     * @return a new task
     */
    public Task<T> failAfter(final long timeout, final TimeUnit timeUnit)
    {
        final Task<T> t = new Task<>();

        // TODO: find a way to inject time for testing
        // also consider letting the application override this with the TaskContext
        final ScheduledFuture<?> rr = schedulerExecutor.schedule(
                () -> {
                    // using t.isDone insteadof this.isDone
                    // because the propagation of this.isDone can be delayed by slow listeners.
                    if (!t.isDone())
                    {
                        t.internalCompleteExceptionally(new TimeoutException());
                    }
                }, timeout, timeUnit);

        this.handle((T v, Throwable ex) -> {
            // removing the scheduled timeout to clear some memory
            rr.cancel(false);
            if (!t.isDone())
            {
                if (ex != null)
                {
                    t.internalCompleteExceptionally(ex);
                }
                else
                {
                    t.internalComplete(v);
                }
            }
            return null;
        });

        return t;
    }


    /**
     * Returns a new task that will fail if the original is not completed withing the given timeout.
     *
     * @param time     the time from now
     * @param timeUnit the time unit of the timeout parameter
     * @return a new task
     */
    public static Task<Void> sleep(final long time, final TimeUnit timeUnit)
    {
        final Task<Void> t = new Task<>();

        // TODO: find a way to inject time for testing
        // also consider letting the application override this with the TaskContext
        final ScheduledFuture<?> rr = schedulerExecutor.schedule(
                () -> {
                    if (!t.isDone())
                    {
                        t.internalComplete(null);
                    }
                }, time, timeUnit);

        return t;
    }

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

        @Override
        public void run()
        {
            try
            {
                while (!task.isDone())
                {
                    try
                    {
                        task.internalComplete(future.get(waitTimeout, waitTimeoutUnit));
                        return;
                    }
                    catch (TimeoutException ex)
                    {
                        if (future.isDone())
                        {
                            // in this case something completed the future with a timeout exception.
                            try
                            {
                                task.internalComplete(future.get(waitTimeout, waitTimeoutUnit));
                                return;
                            }
                            catch (Throwable tex0)
                            {
                                task.internalCompleteExceptionally(tex0);
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
                            // adding the redundant continue here to highlight the code path
                            continue;
                        }
                        catch (Throwable tex)
                        {
                            task.internalCompleteExceptionally(tex);
                            return;
                        }
                    }
                    catch (Throwable ex)
                    {
                        task.internalCompleteExceptionally(ex);
                        return;
                    }
                }
            }
            catch (Throwable ex)
            {
                task.internalCompleteExceptionally(ex);
            }
        }
    }

    public static Task<Void> done()
    {
        final Task<Void> task = new Task<>();
        task.internalComplete(NIL);
        return task;
    }

    @Override
    public <U> Task<U> thenApply(final Function<? super T, ? extends U> fn)
    {
        final Function<? super T, ? extends U> wrap = TaskContext.wrap(fn);
        if (defaultExecutor != null)
        {
            return from(super.thenApplyAsync(wrap, defaultExecutor));
        }
        return from(super.thenApply(wrap));
    }

    @Override
    public Task<Void> thenAccept(final Consumer<? super T> action)
    {
        final Consumer<? super T> wrap = TaskContext.wrap(action);
        if (defaultExecutor != null)
        {
            return from(super.thenAcceptAsync(wrap, defaultExecutor));
        }
        return from(super.thenAccept(wrap));
    }

    @Override
    public Task<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action)
    {
        final BiConsumer<? super T, ? super Throwable> wrap = TaskContext.wrap(action);
        if (defaultExecutor != null)
        {
            return from(super.whenCompleteAsync(wrap, defaultExecutor));
        }
        return from(super.whenComplete(wrap));
    }

    /**
     * Returns a new Task that is executed when this task completes normally.
     * The result of the new Task will be the result of the Supplier passed as parameter.
     * <p/>
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
        // must be separated otherwise the execution of the wrap could happen in another thread
        final Supplier<U> wrap = TaskContext.wrap(supplier);
        if (defaultExecutor != null)
        {
            return from(super.thenApplyAsync(x -> wrap.get(), defaultExecutor));
        }
        return from(super.thenApply(x -> wrap.get()));
    }

    public <U> Task<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)
    {
        final Function<? super T, ? extends CompletionStage<U>> wrap = TaskContext.wrap(fn);
        if (defaultExecutor != null)
        {
            return Task.from(super.thenComposeAsync(wrap, defaultExecutor));
        }
        return Task.from(super.thenCompose(wrap));
    }

    public <U> Task<U> thenCompose(Supplier<? extends CompletionStage<U>> fn)
    {
        // must be separated otherwise the execution of the wrap could happen in another thread
        final Supplier<? extends CompletionStage<U>> wrap = TaskContext.wrap(fn);
        if (defaultExecutor != null)
        {
            return Task.from(super.thenComposeAsync((T x) -> wrap.get(), defaultExecutor));
        }
        return Task.from(super.thenCompose((T x) -> wrap.get()));
    }

    @Override
    public <U> Task<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn)
    {
        final BiFunction<? super T, Throwable, ? extends U> wrap = TaskContext.wrap(fn);
        if (defaultExecutor != null)
        {
            return from(super.handleAsync(wrap, defaultExecutor));
        }
        return from(super.handle(wrap));
    }

    @Override
    public Task<T> exceptionally(final Function<Throwable, ? extends T> fn)
    {
        return from(super.exceptionally(TaskContext.wrap(fn)));
    }

    @Override
    public Task<Void> thenRun(final Runnable action)
    {
        final Runnable wrap = TaskContext.wrap(action);
        if (defaultExecutor != null)
        {
            return from(super.thenRunAsync(wrap, defaultExecutor));
        }
        return from(super.thenRun(wrap));
    }

    @Override
    public Task<java.lang.Void> acceptEither(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer)
    {
        final Consumer<? super T> wrap = TaskContext.wrap(consumer);
        if (defaultExecutor != null)
        {
            return Task.from(super.acceptEitherAsync(completionStage, wrap, defaultExecutor));
        }
        return Task.from(super.acceptEither(completionStage, wrap));
    }

    @Override
    public Task<java.lang.Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer)
    {
        final Consumer<? super T> wrap = TaskContext.wrap(consumer);
        return Task.from(super.acceptEitherAsync(completionStage, wrap));
    }

    @Override
    public Task<java.lang.Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer, Executor executor)
    {
        return Task.from(super.acceptEitherAsync(completionStage, TaskContext.wrap(consumer), executor));
    }

    @Override
    public <U> Task<U> applyToEither(CompletionStage<? extends T> completionStage, Function<? super T, U> function)
    {
        final Function<? super T, U> wrap = TaskContext.wrap(function);
        if (defaultExecutor != null)
        {
            return Task.from(super.applyToEitherAsync(completionStage, wrap, defaultExecutor));
        }
        return Task.from(super.applyToEither(completionStage, wrap));
    }

    @Override
    public <U> Task<U> applyToEitherAsync(CompletionStage<? extends T> completionStage, Function<? super T, U> function, Executor executor)
    {
        return Task.from(super.applyToEitherAsync(completionStage, TaskContext.wrap(function), executor));
    }

    @Override
    public <U> Task<U> applyToEitherAsync(CompletionStage<? extends T> completionStage, Function<? super T, U> function)
    {
        return Task.from(super.applyToEitherAsync(completionStage, TaskContext.wrap(function)));
    }

    @Override
    public <U> Task<U> handleAsync(BiFunction<? super T, java.lang.Throwable, ? extends U> biFunction, Executor executor)
    {
        final BiFunction<? super T, Throwable, ? extends U> wrap = TaskContext.wrap(biFunction);
        return Task.from(super.handleAsync(wrap, executor));
    }

    @Override
    public <U> Task<U> handleAsync(BiFunction<? super T, java.lang.Throwable, ? extends U> biFunction)
    {
        final BiFunction<? super T, Throwable, ? extends U> wrap = TaskContext.wrap(biFunction);
        return Task.from(super.handleAsync(wrap));
    }

    @Override
    public Task<java.lang.Void> runAfterBoth(CompletionStage<?> completionStage, Runnable runnable)
    {
        final Runnable wrap = TaskContext.wrap(runnable);
        if (defaultExecutor != null)
        {
            return Task.from(super.runAfterBothAsync(completionStage, wrap, defaultExecutor));
        }
        return Task.from(super.runAfterBoth(completionStage, wrap));
    }

    @Override
    public Task<java.lang.Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable)
    {
        return Task.from(super.runAfterBothAsync(completionStage, TaskContext.wrap(runnable)));
    }

    @Override
    public Task<java.lang.Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable, Executor executor)
    {
        return Task.from(super.runAfterBothAsync(completionStage, TaskContext.wrap(runnable), executor));
    }

    @Override
    public Task<java.lang.Void> runAfterEither(CompletionStage<?> completionStage, Runnable runnable)
    {
        final Runnable wrap = TaskContext.wrap(runnable);
        if (defaultExecutor != null)
        {
            return Task.from(super.runAfterEitherAsync(completionStage, wrap, defaultExecutor));
        }
        return Task.from(super.runAfterEither(completionStage, wrap));
    }

    @Override
    public Task<java.lang.Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable)
    {
        return Task.from(super.runAfterEitherAsync(completionStage, TaskContext.wrap(runnable)));
    }

    @Override
    public Task<java.lang.Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable, Executor executor)
    {
        return Task.from(super.runAfterEitherAsync(completionStage, TaskContext.wrap(runnable), executor));
    }

    public static Task<java.lang.Void> runAsync(Runnable runnable)
    {
        return Task.from(CompletableFuture.runAsync(TaskContext.wrap(runnable)));
    }

    public static Task<java.lang.Void> runAsync(Runnable runnable, Executor executor)
    {
        return Task.from(CompletableFuture.runAsync(TaskContext.wrap(runnable), executor));
    }

    public static <U> Task<U> supplyAsync(TaskSupplier<U> supplier)
    {
        return Task.from(CompletableFuture.supplyAsync(TaskContext.wrap(supplier))).thenCompose(t -> t);
    }

    public static <U> Task<U> supplyAsync(Supplier<U> supplier)
    {
        return Task.from(CompletableFuture.supplyAsync(TaskContext.wrap(supplier)));
    }

    public static <U> Task<U> supplyAsync(Supplier<U> supplier, Executor executor)
    {
        return Task.from(CompletableFuture.supplyAsync(TaskContext.wrap(supplier), executor));
    }

    public static <U> Task<U> supplyAsync(TaskSupplier<U> supplier, Executor executor)
    {
        return Task.from(CompletableFuture.supplyAsync(TaskContext.wrap(supplier), executor).thenCompose(t -> t));
    }

    @Override
    public Task<java.lang.Void> thenAcceptAsync(Consumer<? super T> consumer, Executor executor)
    {
        return Task.from(super.thenAcceptAsync(TaskContext.wrap(consumer), executor));
    }

    @Override
    public Task<java.lang.Void> thenAcceptAsync(Consumer<? super T> consumer)
    {
        return Task.from(super.thenAcceptAsync(TaskContext.wrap(consumer)));
    }

    @Override
    public <U> Task<java.lang.Void> thenAcceptBoth(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer)
    {
        final BiConsumer<? super T, ? super U> wrap = TaskContext.wrap(biConsumer);
        if (defaultExecutor != null)
        {
            return Task.from(super.thenAcceptBothAsync(completionStage, wrap, defaultExecutor));
        }
        return Task.from(super.thenAcceptBoth(completionStage, wrap));
    }

    @Override
    public <U> Task<java.lang.Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer, Executor executor)
    {
        return Task.from(super.thenAcceptBothAsync(completionStage, TaskContext.wrap(biConsumer), executor));
    }

    @Override
    public <U> Task<java.lang.Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer)
    {
        return Task.from(super.thenAcceptBothAsync(completionStage, TaskContext.wrap(biConsumer)));
    }

    @Override
    public <U> Task<U> thenApplyAsync(Function<? super T, ? extends U> function, Executor executor)
    {
        final Function<? super T, ? extends U> wrap = TaskContext.wrap(function);
        return Task.from(super.thenApplyAsync(wrap, executor));
    }

    @Override
    public <U> Task<U> thenApplyAsync(Function<? super T, ? extends U> function)
    {
        final Function<? super T, ? extends U> wrap = TaskContext.wrap(function);
        return Task.from(super.thenApplyAsync(wrap));
    }

    @Override
    public <U, V> Task<V> thenCombine(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction)
    {
        final BiFunction<? super T, ? super U, ? extends V> wrap = TaskContext.wrap(biFunction);
        if (defaultExecutor != null)
        {
            return Task.from(super.thenCombineAsync(completionStage, wrap, defaultExecutor));
        }
        return Task.from(super.thenCombine(completionStage, wrap));
    }

    @Override
    public <U, V> Task<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction, Executor executor)
    {
        final BiFunction<? super T, ? super U, ? extends V> wrap = TaskContext.wrap(biFunction);
        return Task.from(super.thenCombineAsync(completionStage, wrap, executor));
    }

    @Override
    public <U, V> Task<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction)
    {
        final BiFunction<? super T, ? super U, ? extends V> wrap = TaskContext.wrap(biFunction);
        return Task.from(super.thenCombineAsync(completionStage, wrap));
    }

    @Override
    public <U> Task<U> thenComposeAsync(Function<? super T, ? extends java.util.concurrent.CompletionStage<U>> function, Executor executor)
    {
        final Function<? super T, ? extends CompletionStage<U>> wrap = TaskContext.wrap(function);
        return Task.from(super.thenComposeAsync(wrap, executor));
    }

    @Override
    public <U> Task<U> thenComposeAsync(Function<? super T, ? extends java.util.concurrent.CompletionStage<U>> function)
    {
        final Function<? super T, ? extends CompletionStage<U>> wrap = TaskContext.wrap(function);
        return Task.from(super.thenComposeAsync(wrap));
    }

    @Override
    public Task<java.lang.Void> thenRunAsync(Runnable runnable)
    {
        return Task.from(super.thenRunAsync(TaskContext.wrap(runnable)));
    }

    @Override
    public Task<java.lang.Void> thenRunAsync(Runnable runnable, Executor executor)
    {
        return Task.from(super.thenRunAsync(TaskContext.wrap(runnable), executor));
    }

    @Override
    public Task<T> whenCompleteAsync(BiConsumer<? super T, ? super java.lang.Throwable> biConsumer)
    {
        return Task.from(super.whenCompleteAsync(TaskContext.wrap(biConsumer)));
    }

    @Override
    public Task<T> whenCompleteAsync(BiConsumer<? super T, ? super java.lang.Throwable> biConsumer, Executor executor)
    {
        return Task.from(super.whenCompleteAsync(TaskContext.wrap(biConsumer), executor));
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
    public static <F extends CompletableFuture<?>, C extends Collection<F>> Task<Void> allOf(C cfs)
    {
        return from(CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])));
    }

    /**
     * @throws NullPointerException if the stream or any of its elements are
     *                              {@code null}
     */
    public static <F extends CompletableFuture<?>> Task<Void> allOf(Stream<F> cfs)
    {
        return from(CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new)));
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
    public static <F extends CompletableFuture<?>> Task<Object> anyOf(Collection<F> cfs)
    {
        return from(CompletableFuture.anyOf(cfs.toArray(new CompletableFuture[cfs.size()])));
    }

    /**
     * @throws NullPointerException if the stream or any of its elements are
     *                              {@code null}
     */
    public static <F extends CompletableFuture<?>> Task<Object> anyOf(Stream<F> cfs)
    {
        return from(CompletableFuture.anyOf((CompletableFuture[]) cfs.toArray(CompletableFuture[]::new)));
    }

}
