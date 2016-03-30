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

package cloud.orbit.actors.test;

import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import cloud.orbit.tuples.Pair;

import javax.inject.Singleton;

import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Used for testing
 */
@Singleton
public class FakeSync
{
    private LoadingMap<Object, Task> tasks = new LoadingMap<>(Task::new);

    private LoadingMap<String, UncheckedSemaphore> semaphores = new LoadingMap<>(() -> new UncheckedSemaphore(0));

    private LoadingMap<String, BlockingDeque> deques = new LoadingMap<>(LinkedBlockingDeque::new);

    private LoadingMap<String, CompletableFuture> futures = new LoadingMap<>(CompletableFuture::new);


    // pairs of completable futures and the future completions.
    private Queue<Pair<CompletableFuture, Object>> blockedFutures = new ConcurrentLinkedQueue<>();

    // just calls a function
    public <T> Task<T> futureFrom(Supplier<Task<T>> supplier)
    {
        return supplier.get();
    }

    /**
     * Creates and an uncompleted future and adds it the the queue for later completion.
     * To help with the tests
     */
    public <T> CompletableFuture<T> getBlockedFuture(T value)
    {
        final CompletableFuture<T> future = new CompletableFuture<>();
        blockedFutures.add(Pair.of(future, value));
        return future;
    }

    public <T> CompletableFuture<T> getBlockedFuture()
    {
        return getBlockedFuture(null);
    }

    public <T> Task<T> getBlockedTask(T value)
    {
        final Task<T> future = new Task<>();
        blockedFutures.add(Pair.of(future, value));
        return future;
    }


    public <T> Task<T> getBlockedTask()
    {
        return getBlockedTask(null);
    }

    /**
     * Complete all the blocked futures, even new ones created while executing this method
     */
    public void completeFutures()
    {
        while (blockedFutures.size() > 0)
        {
            final Pair<CompletableFuture, Object> pair = blockedFutures.poll();
            if (pair != null)
            {
                pair.getLeft().complete(pair.getRight());
            }
        }
    }

    public int blockedFutureCount()
    {
        return blockedFutures.size();
    }

    @Deprecated
    public void put(Object key, Object value)
    {
        get(key).complete(value);
    }

    @Deprecated
    public <T> Task<T> get(Object key)
    {
        Task<?> t = tasks.get(key);
        if (t == null)
        {
            tasks.putIfAbsent(key, new Task<>());
            t = tasks.get(key);
        }
        //noinspection unchecked
        return (Task<T>) t;
    }


    public UncheckedSemaphore semaphore(String semaphoreName)
    {
        return semaphores.getOrAdd(semaphoreName);
    }

    public <T> Task<T> task(String name)
    {
        return tasks.getOrAdd(name);
    }

    public <T> CompletableFuture<T> future(String name)
    {
        return futures.getOrAdd(name);
    }

    public <T> BlockingDeque<T> deque(String name)
    {
        return deques.getOrAdd(name);
    }

    private static class LoadingMap<K, V> extends ConcurrentHashMap<K, V>
    {
        private Supplier<V> supplier;

        public LoadingMap(Supplier<V> supplier)
        {
            this.supplier = supplier;
        }

        public V getOrAdd(K key)
        {
            V value = get(key);
            if (value == null)
            {
                final V newValue = supplier.get();
                V oldValue = putIfAbsent(key, newValue);
                return oldValue != null ? oldValue : newValue;
            }
            return value;
        }

    }

    public static class UncheckedSemaphore extends Semaphore
    {
        private UncheckedSemaphore(final int permits)
        {
            super(permits);
        }

        @Override
        public void acquire()
        {
            try
            {
                super.acquire();
            }
            catch (InterruptedException e)
            {
                throw new UncheckedException(e);
            }
        }

        @Override
        public void acquire(final int permits)
        {
            try
            {
                super.acquire(permits);
            }
            catch (InterruptedException e)
            {
                throw new UncheckedException(e);
            }
        }

        public void acquire(final long timeout, final TimeUnit unit)
        {
            try
            {
                if (!super.tryAcquire(timeout, unit))
                {
                    throw new UncheckedException("timeout");
                }
            }
            catch (InterruptedException e)
            {
                throw new UncheckedException(e);
            }
        }


        @Override
        public boolean tryAcquire(final long timeout, final TimeUnit unit)
        {
            try
            {
                return super.tryAcquire(timeout, unit);
            }
            catch (InterruptedException e)
            {
                throw new UncheckedException(e);
            }
        }

        @Override
        public boolean tryAcquire(final int permits, final long timeout, final TimeUnit unit)
        {
            try
            {
                return super.tryAcquire(permits, timeout, unit);
            }
            catch (InterruptedException e)
            {
                throw new UncheckedException(e);
            }
        }
    }
}
