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

package com.ea.orbit.concurrent.test;

import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class TaskTest
{
    @Test
    public void testAllOf()
    {
        Task t1 = new Task();
        Task t2 = new Task();
        Task t3 = new Task();
        Task all = Task.allOf(t1, t2, t3);

        assertFalse(all.isDone());
        t1.complete(1);
        assertFalse(all.isDone());

        t2.complete(2);
        t3.complete(3);
        assertTrue(all.isDone());
    }

    @Test
    public void testAllOfWithError()
    {
        Task t1 = new Task();
        Task t2 = new Task();
        Task t3 = new Task();
        Task all = Task.allOf(t1, t2, t3);

        assertFalse(all.isDone());
        t1.complete(1);
        assertFalse(all.isDone());

        // simulating an error
        t2.completeExceptionally(new RuntimeException());
        assertFalse(all.isDone());
        t3.complete(3);
        // ensuring that allOf only completes after all subs are completed, even if there were errors.
        assertTrue(all.isDone());
    }

    @Test
    public void testAllOfVariations()
    {
        Task<Integer> t1 = new Task();
        Task t2 = new Task();
        Task t3 = new Task();
        CompletableFuture c4 = new CompletableFuture();
        Task all_regular = Task.allOf(t1, t2, t3);
        Task all_array = Task.allOf(new CompletableFuture[]{t1, t2, t3});
        Task all_array2 = Task.allOf(new Task[]{t1, t2, t3});
        Task all_collection = Task.allOf(Arrays.asList(t1, t2, t3));
        Task all_stream = Task.allOf(Arrays.asList(t1, t2, t3).stream());
        Stream<Task> stream = Arrays.asList(t1, t2, t3).stream();
        Task all_stream2 = Task.allOf(stream);
        Task all_stream3 = Task.allOf(Arrays.asList(c4).stream());
        Stream<CompletableFuture> stream4 = Arrays.asList(t1, t2, t3, c4).stream();
        Task all_stream4 = Task.allOf(stream4);
        Task all_stream5 = Task.allOf(Arrays.asList(t1, t2, t3, c4).stream());

        t1.complete(1);
        t2.completeExceptionally(new RuntimeException());
        t3.complete(3);
        c4.complete(4);
        assertTrue(all_regular.isDone());
        assertTrue(all_array.isDone());
        assertTrue(all_array2.isDone());
        assertTrue(all_stream.isDone());
        assertTrue(all_stream2.isDone());
        assertTrue(all_stream3.isDone());
        assertTrue(all_stream4.isDone());
        assertTrue(all_stream5.isDone());
        assertTrue(all_collection.isDone());
    }

    @Test
    public void testAnyOfVariations()
    {
        Task<Integer> t1 = new Task();
        Task t2 = new Task();
        Task t3 = new Task();
        CompletableFuture c4 = new CompletableFuture();
        Task group_regular = Task.anyOf(t1, t2, t3);
        Task group_array = Task.anyOf(new CompletableFuture[]{t1, t2, t3});
        Task group_array2 = Task.anyOf(new Task[]{t1, t2, t3});
        Task group_collection = Task.anyOf(Arrays.asList(t1, t2, t3));
        Task group_stream = Task.anyOf(Arrays.asList(t1, t2, t3).stream());
        Stream<Task> stream = Arrays.asList(t1, t2, t3).stream();
        Task group_stream2 = Task.anyOf(stream);
        Task group_stream3 = Task.anyOf(Arrays.asList(c4).stream());
        Stream<CompletableFuture> stream4 = Arrays.asList(t1, t2, t3, c4).stream();
        Task group_stream4 = Task.anyOf(stream4);
        Task group_stream5 = Task.anyOf(Arrays.asList(t1, t2, t3, c4).stream());

        t1.complete(1);
        c4.complete(4);
        assertTrue(group_regular.isDone());
        assertTrue(group_array.isDone());
        assertTrue(group_array2.isDone());
        assertTrue(group_stream.isDone());
        assertTrue(group_stream2.isDone());
        assertTrue(group_stream3.isDone());
        assertTrue(group_stream4.isDone());
        assertTrue(group_stream5.isDone());
        assertTrue(group_collection.isDone());
    }

    @Test
    public void testThenApply()
    {
        Task<Integer> t1 = new Task();
        Task<String> t2 = t1.thenApply(x -> "a");
        assertFalse(t1.isDone());
        t1.complete(1);
        assertTrue(t2.isDone());
        assertEquals("a", t2.join());
    }

    @Test
    public void testThenApplyWithVoid()
    {
        Task<Void> t1 = new Task();
        Task<String> t2 = t1.thenApply(x -> "a");
        assertFalse(t1.isDone());
        t1.complete(null);
        assertTrue(t2.isDone());
        assertEquals("a", t2.join());
    }

    @Test
    public void testThenReturn()
    {
        Task<Integer> t1 = new Task();
        Task<String> t2 = t1.thenReturn(() -> "a");
        assertFalse(t1.isDone());
        t1.complete(1);
        assertTrue(t2.isDone());
        assertEquals("a", t2.join());
    }


    @Test
    public void testThenCompose()
    {
        Task<Integer> t1 = new Task();
        Task<String> t2 = t1.thenCompose(x -> Task.fromValue(x + "a"));
        Task<String> t3 = t1.thenCompose(x -> CompletableFuture.completedFuture(x + "a"));
        assertFalse(t1.isDone());
        t1.complete(1);
        assertTrue(t2.isDone());
        assertEquals("1a", t2.join());
        assertEquals("1a", t3.join());
    }

    @Test
    public void testThenComposeNoParams()
    {
        Task<Integer> t1 = new Task();
        Task<String> t2 = t1.thenCompose(() -> Task.fromValue("b"));
        Task<String> t3 = t1.thenCompose(() -> CompletableFuture.completedFuture("c"));
        assertFalse(t1.isDone());
        t1.complete(1);
        assertTrue(t2.isDone());
        assertEquals("b", t2.join());
        assertEquals("c", t3.join());
    }


    @Test
    public void testThenReturnWithException()
    {
        Task<Integer> t1 = new Task();
        Task<String> t2 = t1.thenReturn(() -> "a");
        assertFalse(t1.isDone());
        t1.completeExceptionally(new RuntimeException());
        assertTrue(t2.isDone());
        assertTrue(t2.isCompletedExceptionally());
    }

    @Test
    public void testGetAndJoinWithWrappedForkJoinTask() throws ExecutionException, InterruptedException
    {
        final ForkJoinTask<String> task = ForkJoinTask.adapt(() -> "bla");
        final Task<String> t1 = Task.fromFuture(task);
        assertFalse(t1.isDone());
        task.invoke();
        assertEquals("bla", t1.join());
        assertTrue(t1.isDone());
        assertEquals("bla", t1.join());
        assertEquals("bla", t1.get());
    }


    @Test
    public void testGetAndJoinWithWrappedForkJoinTaskAndTimeout() throws ExecutionException, InterruptedException
    {
        final ForkJoinTask<String> task = ForkJoinTask.adapt(() -> "bla");
        final Task<String> t1 = Task.fromFuture(task);
        assertFalse(t1.isDone());
        Thread.sleep(10);
        task.fork();
        assertEquals("bla", t1.join());
        assertTrue(t1.isDone());
        assertEquals("bla", t1.join());
        assertEquals("bla", t1.get());
    }
}
