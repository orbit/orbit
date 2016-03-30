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

package cloud.orbit.concurrent.test;

import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class TaskImplementationTest
{
    @SuppressWarnings("deprecation")
    private static class CTask<T> extends Task<T>
    {
        @Override
        public boolean complete(T value)
        {
            return super.internalComplete(value);
        }

        @Override
        public boolean completeExceptionally(Throwable ex)
        {
            return super.internalCompleteExceptionally(ex);
        }
    }

    @Test
    public void testAllOf()
    {
        CTask<Integer> t1 = new CTask<>();
        CTask<Integer> t2 = new CTask<>();
        CTask<Integer> t3 = new CTask<>();
        Task<?> all = Task.allOf(t1, t2, t3);

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
        CTask<Integer> t1 = new CTask<>();
        CTask<Integer> t2 = new CTask<>();
        CTask<Integer> t3 = new CTask<>();
        Task<?> all = Task.allOf(t1, t2, t3);

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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testAllOfVariations()
    {
        CTask<Integer> t1 = new CTask();
        CTask t2 = new CTask();
        CTask t3 = new CTask();
        CompletableFuture c4 = new CompletableFuture();
        Task all_regular = CTask.allOf(t1, t2, t3);
        Task all_array = CTask.allOf(new CompletableFuture[]{ t1, t2, t3 });
        Task all_array2 = CTask.allOf(new CTask[]{ t1, t2, t3 });
        Task all_collection = CTask.allOf(Arrays.asList(t1, t2, t3));
        Task all_stream = CTask.allOf(Arrays.asList(t1, t2, t3).stream());
        Stream<CTask> stream = Arrays.asList(t1, t2, t3).stream();
        Task all_stream2 = CTask.allOf(stream);
        Task all_stream3 = CTask.allOf(Arrays.asList(c4).stream());
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testAnyOfVariations()
    {
        CTask<Integer> t1 = new CTask<>();
        CTask t2 = new CTask<>();
        CTask<?> t3 = new CTask<>();
        CompletableFuture c4 = new CompletableFuture();
        Task group_regular = CTask.anyOf(t1, t2, t3);
        Task group_array = CTask.anyOf(new CompletableFuture[]{ t1, t2, t3 });
        Task group_array2 = CTask.anyOf(new CTask[]{ t1, t2, t3 });
        Task group_collection = CTask.anyOf(Arrays.asList(t1, t2, t3));
        Task group_stream = CTask.anyOf(Arrays.asList(t1, t2, t3).stream());
        Stream<CTask> stream = Arrays.asList(t1, t2, t3).stream();
        Task group_stream2 = CTask.anyOf(stream);
        Task group_stream3 = CTask.anyOf(Arrays.asList(c4).stream());
        Stream<CompletableFuture> stream4 = Arrays.asList(t1, t2, t3, c4).stream();
        Task group_stream4 = CTask.anyOf(stream4);
        Task group_stream5 = CTask.anyOf(Arrays.asList(t1, t2, t3, c4).stream());

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
        CTask<Integer> t1 = new CTask<>();
        Task<String> t2 = t1.thenApply(x -> "a");
        assertFalse(t1.isDone());
        t1.complete(1);
        assertTrue(t2.isDone());
        assertEquals("a", t2.join());
    }

    @Test
    public void testThenApplyWithVoid()
    {
        CTask<Void> t1 = new CTask<>();
        Task<String> t2 = t1.thenApply(x -> "a");
        assertFalse(t1.isDone());
        t1.complete(null);
        assertTrue(t2.isDone());
        assertEquals("a", t2.join());
    }

    @Test
    public void testThenReturn()
    {
        CTask<Integer> t1 = new CTask<>();
        Task<String> t2 = t1.thenReturn(() -> "a");
        assertFalse(t1.isDone());
        t1.complete(1);
        assertTrue(t2.isDone());
        assertEquals("a", t2.join());
    }


    @Test
    public void testThenCompose()
    {
        CTask<Integer> t1 = new CTask<>();
        Task<String> t2 = t1.thenCompose(x -> CTask.fromValue(x + "a"));
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
        CTask<Integer> t1 = new CTask<>();
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
        CTask<Integer> t1 = new CTask<>();
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

    @Test
    public void testSleep()
    {
        long start = System.currentTimeMillis();
        final Task<Void> sleep = Task.sleep(20, TimeUnit.MILLISECONDS);
        assertFalse(sleep.isDone());
        final Task<Long> completionTime = sleep.thenApply(x -> System.currentTimeMillis());
        assertTrue(completionTime.join() - start >= 20);
        assertTrue(sleep.isDone());
    }
}
