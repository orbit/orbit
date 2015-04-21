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
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ea.orbit.async.Await.await;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LoopTest
{
    static
    {
        Await.init();
    }

    // pairs of completable futures and the future completions.
    Queue<Pair<CompletableFuture, Object>> blockedFutures = new LinkedList<>();

    // just calls a function
    <T> Task<T> futureFrom(Supplier<Task<T>> supplier)
    {
        return supplier.get();
    }

    // creates and an uncompleted future and adds it the the queue for later completion.
    // to help with the tests
    public <T> CompletableFuture<T> getBlockedFuture(T value)
    {
        final CompletableFuture<T> future = new CompletableFuture<>();
        blockedFutures.add(Pair.of(future, value));
        return future;
    }

    void completeFutures()
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


    @Test
    public void testForLoop()
    {
        int count = 5;
        final Task<Object> task = futureFrom(() -> {
            String str = "";
            for (int i = 0; i < count; i++)
            {
                str += ":" + await(getBlockedFuture(i));
            }
            return Task.fromValue(str);
        });
        assertFalse(task.isDone());
        completeFutures();
        assertTrue(task.isDone());
        assertEquals(":0:1:2:3:4", task.join());
    }

    @Test
    public void testWhileLoop()
    {
        int count = 5;
        final Task<Object> task = futureFrom(() -> {
            String str = "";
            int i = 0;
            while (i < count)
            {
                str += ":" + await(getBlockedFuture(i));
                i++;
            }
            return Task.fromValue(str);
        });
        assertFalse(task.isDone());
        completeFutures();
        assertTrue(task.isDone());
        assertEquals(":0:1:2:3:4", task.join());
    }


    @Test
    public void testDoLoop()
    {
        int count = 5;
        final Task<Object> task = futureFrom(() -> {
            String str = "";
            int i = 0;
            do
            {
                str += ":" + await(getBlockedFuture(i));
                i++;
            } while (i < count);
            return Task.fromValue(str);
        });
        assertFalse(task.isDone());
        completeFutures();
        assertTrue(task.isDone());
        assertEquals(":0:1:2:3:4", task.join());
    }


    @Test
    public void testForEach()
    {
        final List<CompletableFuture<Integer>> blockedFuts = IntStream.range(0, 5)
                .mapToObj(i -> getBlockedFuture((Integer) i))
                .collect(Collectors.toList());
        final Task<Object> task = futureFrom(() -> {
            String str = "";
            for (CompletableFuture f : blockedFuts)
            {
                str += ":" + await(f);
            }
            return Task.fromValue(str);
        });
        assertFalse(task.isDone());
        completeFutures();
        assertTrue(task.isDone());
        assertEquals(":0:1:2:3:4", task.join());
    }


}
