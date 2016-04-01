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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.cloner.KryoCloner;
import cloud.orbit.profiler.ProfilerData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A mini profile extension to measure the actor method cpu usage.
 */
public class ActorProfiler
{
    private final HashMap<Object, ProfilerData> collectedData = new HashMap<>();
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final KryoCloner kryoCloner = new KryoCloner();
    protected int collectInterval;
    protected TimeUnit collectTimeUnit = TimeUnit.MILLISECONDS;
    protected double collectionTotalTimeNanos;
    protected long collectionCount;

    public void collect()
    {
        long collectionStart = System.nanoTime();
        rwlock.writeLock().lock();
        try
        {
            // snapshots taken over a large number of samples

            final Thread currentThread = Thread.currentThread();
            final Thread[] tarray = new Thread[Thread.activeCount()];
            Thread.enumerate(tarray);

            // to be sure we have all, in case the actor thread group is separated.
            for (Thread thread : tarray)
            {
                if (thread == null || thread == currentThread)
                {
                    continue;
                }
                final ActorTaskContext context1 = ActorTaskContext.currentFor(thread);
                final StackTraceElement[] stackTrace = thread.getStackTrace();
                final ActorTaskContext context2 = ActorTaskContext.currentFor(thread);
                Object key = null;
                if (context1 != null && context1 == context2 && context1.getActor() != null)
                {
                    final RemoteReference reference = RemoteReference.from(context1.getActor());
                    key = reference._interfaceClass();
                }
                else
                {
                    for (int ik = stackTrace.length; --ik >= 0; )
                    {
                        final StackTraceElement traceElement = stackTrace[ik];
                        if (traceElement != null
                                && traceElement.getClassName() != null
                                && traceElement.getClassName().startsWith("cloud.orbit"))
                        {
                            key = "orbit-" + thread.getState();
                            break;
                        }
                    }
                    if (key == null)
                    {
                        key = "global-" + thread.getState();
                    }
                }
                ProfilerData profilerData = collectedData.get(key);
                if (profilerData == null)
                {
                    profilerData = new ProfilerData();
                    collectedData.put(key, profilerData);
                }
                cleanStackTrack(stackTrace);
                profilerData.collect(stackTrace);
            }
        }
        finally
        {
            long collectionEnd = System.nanoTime();
            collectionTotalTimeNanos += collectionEnd - collectionStart;
            collectionCount++;
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Interrupts the collection and takes a copy of the collected stats
     */
    public Map<Object, ProfilerData> getProfilerSnapshot()
    {
        rwlock.readLock().lock();
        try
        {
            return kryoCloner.clone(collectedData);
        }
        finally
        {
            rwlock.readLock().unlock();
        }
    }

    protected void cleanStackTrack(final StackTraceElement[] stackTrace)
    {
        // remove any methods that don't add value to the collection
    }

    public int getCollectInterval()
    {
        return collectInterval;
    }

    public void setCollectInterval(final int collectInterval)
    {
        this.collectInterval = collectInterval;
    }

    public TimeUnit getCollectTimeUnit()
    {
        return collectTimeUnit;
    }

    public void setCollectTimeUnit(final TimeUnit collectTimeUnit)
    {
        this.collectTimeUnit = collectTimeUnit;
    }

    public double getCollectionTotalTimeNanos()
    {
        rwlock.readLock().lock();
        try
        {
            return collectionTotalTimeNanos;
        }
        finally
        {
            rwlock.readLock().unlock();
        }
    }

    public long getCollectionCount()
    {
        rwlock.readLock().lock();
        try
        {
            return collectionCount;
        }
        finally
        {
            rwlock.readLock().unlock();
        }
    }

    public void clear()
    {
        rwlock.writeLock().lock();
        try
        {
            collectionCount = 0;
            collectionTotalTimeNanos = 0;
            collectedData.clear();
        }
        finally
        {
            rwlock.writeLock().unlock();
        }
    }
}
