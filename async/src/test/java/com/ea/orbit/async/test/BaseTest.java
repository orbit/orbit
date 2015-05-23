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

package com.ea.orbit.async.test;

import com.ea.orbit.async.Await;
import com.ea.orbit.concurrent.Task;

import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class BaseTest
{
    static
    {
        Await.init();
    }

    // pairs of completable futures and the future completions.
    protected Queue<Pair<CompletableFuture, Object>> blockedFutures = new LinkedList<>();

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
                pair.getKey().complete(pair.getValue());
            }
        }
    }

    /**
     * Shortcut to create tests with lambda functions
     */
    protected <T, V> Task<V> call(T t, Function<T, Task<V>> function)
    {
        return function.apply(t);
    }

    /**
     * Shortcut to create tests with lambda functions
     */
    protected <V> Task<V> call(Callable<Task<V>> function) throws Exception
    {
        return function.call();
    }
}
