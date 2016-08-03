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

package cloud.orbit.actors.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import cloud.orbit.actors.runtime.InternalUtils;
import cloud.orbit.concurrent.ExecutorUtils;
import cloud.orbit.concurrent.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Ensures that only a single task is executed at each time per key.
 *
 * @author Daniel Sperry
 */
public class WaitFreeMultiExecutionSerializer<T> implements MultiExecutionSerializer<T>
{
    private static final Logger logger = LoggerFactory.getLogger(WaitFreeMultiExecutionSerializer.class);

    private final ExecutorService executorService;

    // while running, the WaitFreeExecutionSerializer is held alive by references from the executorService
    // and from anyone holding the promises (Tasks) it returns.
    private final Cache<T, WaitFreeExecutionSerializer> serializers = Caffeine.newBuilder().weakValues().build();

    public WaitFreeMultiExecutionSerializer()
    {
        this(ExecutorUtils.newScalingThreadPool(ForkJoinPool.getCommonPoolParallelism()));
    }

    public WaitFreeMultiExecutionSerializer(final ExecutorService executor)
    {
        this.executorService = executor;
    }

    public WaitFreeExecutionSerializer getSerializer(T key)
    {
        return serializers.get(key, o -> new WaitFreeExecutionSerializer(executorService, key));
    }

    /**
     * Only accepts if the queue size is not exceeded.
     *
     * @return true if the task was accepted.
     */
    @Override
    public <R> Task<R> offerJob(T key, Supplier<Task<R>> job, int maxQueueSize)
    {
        // todo remove this.
        if (key == null)
        {
            executorService.execute(() -> InternalUtils.safeInvoke(job));
        }
        return getSerializer(key).executeSerialized(() -> InternalUtils.safeInvoke(job), maxQueueSize);
    }

    @Override
    public void shutdown()
    {
        executorService.shutdown(); // Disable new tasks from being submitted
        try
        {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
            {
                logger.info("Timeout elapsed before termination, forcing shutdown");
                List<Runnable> tasksAwaitingExecution = executorService.shutdownNow(); // Cancel currently executing tasks
                logger.info("Tasks awaiting execution after forced shutdown: " + tasksAwaitingExecution.size());
            }
        }
        catch (InterruptedException ie)
        {
            logger.error("Exception occurred while shutting down thread pool", ie);
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        logger.info("Thread pool shutdown complete");
    }

    @Override
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