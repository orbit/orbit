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

import com.ea.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Ensures that only a single task is executed at each time.
 */
public class ExecutionSerializer2
{
    private static final Logger logger = LoggerFactory.getLogger(ExecutionSerializer2.class);
    private ExecutorService executorService;
    private ConcurrentLinkedQueue<Supplier<Task<?>>> queue = new ConcurrentLinkedQueue<>();
    private AtomicBoolean lock = new AtomicBoolean();

    public ExecutionSerializer2(final ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    public boolean executeSerialized(Supplier<Task<?>> taskSupplier, int maxQueueSize)
    {
        if (queue.size() >= maxQueueSize)
        {
            return false;
        }
        if (!queue.add(taskSupplier))
        {
            return false;
        }
        tryExecute();
        return true;
    }

    private void tryExecute()
    {
        do
        {
            if (!lock.compareAndSet(false, true))
            {
                // some other thread has the lock and it is now responsible for draining the queue.
                return;
            }

            // while there is something in the queue
            for (Supplier<Task<?>> toRun; null != (toRun = queue.poll()); )
            {
                Task<?> taskFuture;
                try
                {
                    taskFuture = toRun.get();
                }
                catch (Throwable error)
                {
                    taskFuture = null;
                    try
                    {
                        logger.error("Error executing action", error);
                    }
                    catch (Throwable ex)
                    {
                        // just to be on the safe side... loggers can fail...
                        lock.compareAndSet(true, false);
                        ex.printStackTrace();
                        return;
                    }
                }
                if (taskFuture != null && !taskFuture.isDone())
                {
                    // this will run whenComplete in another thread
                    taskFuture.whenCompleteAsync(this::whenCompleteAsync, executorService);
                    // returning without unlocking, onComplete will do it;
                    return;
                }
                else
                {
                    // was executed immediately
                    // unlock
                    lock.compareAndSet(true, false);
                }
            }
        } while (!queue.isEmpty());
    }

    private <T> void whenCompleteAsync(T result, Throwable error)
    {
        // unlock
        lock.compareAndSet(true, false);
        // double take:
        // try executing again, in case some new data arrived
        tryExecute();
    }
}
