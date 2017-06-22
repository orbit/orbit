/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.runtime.InternalUtils;
import cloud.orbit.concurrent.Task;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Supplier;

/**
 * Created by joeh on 2017-05-10.
 */
public class ConcurrentExecutionQueue implements Executor
{
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentExecutionQueue.class);

    private final ExecutorService executorService;
    private final Integer concurrentExecutions;
    private final Integer maxQueueSize;
    private static final AtomicIntegerFieldUpdater<ConcurrentExecutionQueue> lockUpdater = AtomicIntegerFieldUpdater.newUpdater(ConcurrentExecutionQueue.class, "lock");
    private static final AtomicIntegerFieldUpdater<ConcurrentExecutionQueue> queueSizeUpdater = AtomicIntegerFieldUpdater.newUpdater(ConcurrentExecutionQueue.class, "queueSize");
    private static final AtomicIntegerFieldUpdater<ConcurrentExecutionQueue> inFlightUpdater = AtomicIntegerFieldUpdater.newUpdater(ConcurrentExecutionQueue.class, "inFlight");
    private final ConcurrentLinkedQueue<Supplier<Task<?>>> queue = new ConcurrentLinkedQueue<>();

    @SuppressWarnings("FieldCanBeLocal")
    private volatile int lock = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private volatile int queueSize = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private volatile int inFlight = 0;

    public ConcurrentExecutionQueue(final ExecutorService executorService, final int concurrentExecutions)
    {
        this(executorService, concurrentExecutions, 0);
    }

    public ConcurrentExecutionQueue(final ExecutorService executorService, final int concurrentExecutions, final int maxQueueSize)
    {
        this.executorService = executorService;
        this.concurrentExecutions = concurrentExecutions;
        this.maxQueueSize = maxQueueSize;
    }

    public <R> Task<R> execute(Supplier<Task<R>> taskSupplier)
    {
        final Task<R> completion = new Task<>();

        if ((maxQueueSize > 0 && queueSize >= maxQueueSize) || !queue.add(() -> {
            Task<R> source = InternalUtils.safeInvoke(taskSupplier);
            InternalUtils.linkFutures(source, completion);
            return source;
        }))
        {
            throw new IllegalStateException(String.format("Queue full (%d > %d)", queue.size(), maxQueueSize));
        }

        queueSizeUpdater.incrementAndGet(this);

        tryDrainQueue();

        return completion;
    }

    private void tryDrainQueue()
    {
        while(!queue.isEmpty() && inFlight < concurrentExecutions) {
            if (!lock())
            {
                // some other thread has the lock and it is now responsible for draining the queue.
                return;
            }

            // while there is something in the queue
            final Supplier<Task<?>> toRun = queue.poll();
            if (toRun != null)
            {
                inFlightUpdater.incrementAndGet(this);
                queueSizeUpdater.decrementAndGet(this);

                try {
                    final Task<?> task = new Task<>();
                    executorService.execute(() -> {
                        wrapExecution(toRun, task);
                        if (task.isDone())
                        {
                            inFlightUpdater.decrementAndGet(this);
                            tryDrainQueue();
                        }
                        else
                        {
                            task.whenCompleteAsync(ConcurrentExecutionQueue.this::whenCompleteAsync, executorService);
                        }
                    });
                }
                catch (Throwable ex)
                {
                    inFlightUpdater.decrementAndGet(this);
                    try
                    {
                        logger.error("Error executing action", ex);
                    }
                    catch (Throwable ex2)
                    {
                        // just to be on the safe side... loggers can fail...
                        ex2.printStackTrace();
                        ex.printStackTrace();
                    }
                }
            }

            unlock();
        }
    }

    private <T> void whenCompleteAsync(T result, Throwable error)
    {
        inFlightUpdater.decrementAndGet(this);
        tryDrainQueue();
    }

    @Override
    public void execute(final Runnable command)
    {
        execute(() -> {
            command.run();
            return Task.done();
        });
    }

    private void wrapExecution(final Supplier<Task<?>> toRun, final Task<?> taskFuture)
    {
        try
        {
            final Task<?> task = (Task) toRun.get();
            if (task == null || task.isDone())
            {
                taskFuture.complete(null);
            }
            else
            {
                task.whenComplete((r, e) -> {
                    if (e != null)
                    {
                        taskFuture.completeExceptionally(e);
                    }
                    else
                    {
                        taskFuture.complete(null);
                    }
                });
            }
        }
        catch (Exception e)
        {
            taskFuture.completeExceptionally(e);
        }
    }

    private void unlock()
    {
        if (!lockUpdater.compareAndSet(this, 1, 0))
        {
            logger.error("Unlocking without having the lock");
        }
    }

    private boolean lock()
    {
        return lockUpdater.compareAndSet(this, 0, 1);
    }
}
