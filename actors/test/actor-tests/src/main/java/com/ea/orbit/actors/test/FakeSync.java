package com.ea.orbit.actors.test;

import com.ea.orbit.concurrent.Task;
import com.ea.orbit.tuples.Pair;

import javax.inject.Singleton;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Used for testing
 */
@Singleton
public class FakeSync
{
    private ConcurrentHashMap<Object, Task<?>> shared = new ConcurrentHashMap<>();

    // pairs of completable futures and the future completions.
    private Queue<Pair<CompletableFuture, Object>> blockedFutures = new LinkedList<>();

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

    public void put(Object key, Object value)
    {
        get(key).complete(value);
    }

    public void putException(Object key, Throwable value)
    {
        get(key).completeExceptionally(value);
    }

    public <T> Task<T> get(Object key)
    {
        Task<?> t = shared.get(key);
        if (t == null)
        {
            shared.putIfAbsent(key, new Task<>());
            t = shared.get(key);
        }
        //noinspection unchecked
        return (Task<T>) t;
    }
}
