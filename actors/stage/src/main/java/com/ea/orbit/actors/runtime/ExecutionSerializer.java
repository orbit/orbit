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

package com.ea.orbit.actors.runtime;

import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Ensures that only a single task is executed at each time per key.
 */
public class ExecutionSerializer<T>
{
    private static final Logger logger = LoggerFactory.getLogger(Execution.class);
    private ExecutorService executorService;
    private Map<Object, Runner> running = new HashMap<>();
    private final Object mutex = new Object();

    public ExecutionSerializer()
    {
        executorService = ExecutorUtils.newScalingThreadPool(1000);
    }

    public ExecutionSerializer(final ExecutorService executor)
    {
        this.executorService = executor;
    }

    protected class Runner implements Runnable
    {
        Queue<Supplier<Task<?>>> queue = new LinkedBlockingQueue<>();
        T key;

        public void run()
        {
            do
            {
                try
                {
                    final Supplier<Task<?>> taskSupplier = queue.remove();

                    // actual runtime of the supplier
                    final Task<?> task = taskSupplier.get();
                    if (task != null && !task.isDone())
                    {
                        // if the task is not complete then it's completion will restart the sequential runtime.
                        // since onComplete is only called when the tasks are completed, then the runner will never be
                        // executing in parallel.
                        task.whenCompleteAsync((r, e) -> {
                            if (!onComplete(Runner.this))
                            {
                                Runner.this.run();
                            }
                        }, executorService);
                        return;
                    }
                }
                catch (Throwable throwable)
                {
                    // this should not be possible as exceptions should be caught by the sub tasks.
                    if (logger.isErrorEnabled())
                    {
                        logger.error("Error executing a sequential task: " + key, throwable);
                    }
                }
                // this is the loop for tasks that finish immediately
            } while (!onComplete(Runner.this));
        }
    }

    /**
     * Only accepts if the queue size is not exceeded.
     *
     * @return true if the task was accepted.
     */
    public boolean offerJob(T key, Supplier<Task<?>> run, int maxQueueSize)
    {
        if (key == null)
        {
            executorService.execute(() -> run.get());
            return true;
        }
        synchronized (mutex)
        {
            Runner runner = running.get(key);
            if (runner != null)
            {
                if (runner.queue.size() >= maxQueueSize)
                {
                    return false;
                }
                runner.queue.add(run);
            }
            else
            {
                runner = new Runner();
                runner.key = key;
                running.put(key, runner);
                runner.queue.add(run);
                executorService.execute(runner);
            }
        }
        return true;
    }

    /**
     * Makes sure the runner is empty before removing it from the map.
     *
     * @param runner
     * @return false if the runner queue is not empty.
     */
    protected boolean onComplete(Runner runner)
    {
        synchronized (mutex)
        {
            if (runner.queue.size() > 0)
            {
                // something was added right before the synchronization.
                return false;
            }
            else
            {
                running.remove(runner.key);
                return true;
            }
        }
    }

    public void shutdown()
    {
        executorService.shutdown();
    }
}
