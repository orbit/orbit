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

package com.ea.orbit.actors.concurrent;

import com.ea.orbit.actors.runtime.InternalUtils;
import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Ensures that only a single task is executed at each time per key.
 *
 * @author Daniel Sperry
 */
public class WaitFreeMultiExecutionSerializer<T> implements MultiExecutionSerializer<T>
{
    private static final Logger logger = LoggerFactory.getLogger(WaitFreeMultiExecutionSerializer.class);
    private ExecutorService executorService;

    // while running, the WaitFreeExecutionSerializer is held alive by references from the executorService
    // and from anyone holding the promises (Tasks) it returns.
    private Cache<T, WaitFreeExecutionSerializer> serializers = CacheBuilder.newBuilder().weakValues().build();

    public WaitFreeMultiExecutionSerializer()
    {
        executorService = ExecutorUtils.newScalingThreadPool(ForkJoinPool.getCommonPoolParallelism());
    }

    public WaitFreeMultiExecutionSerializer(final ExecutorService executor)
    {
        this.executorService = executor;
    }

    public WaitFreeExecutionSerializer getSerializer(T key)
    {
        final WaitFreeExecutionSerializer serializer = serializers.getIfPresent(key);
        if (serializer == null)
        {
            try
            {
                return serializers.get(key, () -> new WaitFreeExecutionSerializer(executorService));
            }
            catch (ExecutionException e)
            {
                throw new UncheckedException(e);
            }
        }
        return serializer;
    }

    /**
     * Only accepts if the queue size is not exceeded.
     *
     * @return true if the task was accepted.
     */
    public <R> Task<R> offerJob(T key, Supplier<Task<R>> job, int maxQueueSize)
    {
        // todo remove this.
        if (key == null)
        {
            executorService.execute(() -> InternalUtils.safeInvoke(job));
        }
        return getSerializer(key).executeSerialized(() -> InternalUtils.safeInvoke(job), maxQueueSize);
    }

    public void shutdownNow()
    {
        List<Runnable> tasksAwaitingExecution = executorService.shutdownNow();
        if (!tasksAwaitingExecution.isEmpty())
        {
            logger.warn("Tasks awaiting execution after shutdown: " + tasksAwaitingExecution.size());
        }
    }

    public boolean isBusy()
    {
        for (WaitFreeExecutionSerializer serializer : serializers.asMap().values())
        {
            if (serializer.isBusy())
            {
                return true;
            }
        }
        return false;
    }
}
