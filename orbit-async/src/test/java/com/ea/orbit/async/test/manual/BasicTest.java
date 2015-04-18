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

package com.ea.orbit.async.test.manual;

import com.ea.orbit.async.Async;
import com.ea.orbit.async.instrumentation.InstrumentAsync;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class BasicTest
{
    public interface ISomethingAsync
    {
        CompletableFuture<Object> doSomething(CompletableFuture<String> blocker);
    }

    public static class SomethingAsync implements ISomethingAsync
    {
        @Async
        public CompletableFuture<Object> doSomething(CompletableFuture<String> blocker)
        {
            String res = blocker.join();
            return CompletableFuture.completedFuture(":" + res);
        }
    }

    public static class SomethingWithLocalsAndStack implements ISomethingAsync
    {
        @Async
        public CompletableFuture<Object> doSomething(CompletableFuture<String> blocker)
        {
            int local = 7;
            String res = ":" + Math.max(local, blocker.join().length());
            return CompletableFuture.completedFuture(res);
        }
    }

    public static class SomethingAsyncWithException implements ISomethingAsync
    {
        @Async
        public CompletableFuture<Object> doSomething(CompletableFuture<String> blocker)
        {
            try
            {
                String res = blocker.join();
                return CompletableFuture.completedFuture(":" + res);
            }
            catch (Exception ex)
            {
                return CompletableFuture.completedFuture(":" + ex.getCause().getMessage());
            }
        }
    }

    private static class SomethingNotAsync
    {
        public void blah()
        {
        }
    }

    @Test
    public void testRunningInstrumentation()
    {
        // runs a instrumentation ach check if a new class is created when appropriated
        InstrumentAsync ins = new InstrumentAsync();
        Class<SomethingAsync> newClass = ins.instrument(SomethingAsync.class);
        assertNotNull(newClass);
        //assertNotSame(SomethingAsync.class, newClass);
        assertSame(SomethingNotAsync.class, ins.instrument(SomethingNotAsync.class));
        assertTrue(ISomethingAsync.class.isAssignableFrom(newClass));
    }

    @Test
    public void testDirectPathNonBlocking() throws IllegalAccessException, InstantiationException
    {
        // test an example where the async function blocks (returns incomplete future)
        // this would not work without instrumentation
        InstrumentAsync ins = new InstrumentAsync();
        Class<?> newClass = ins.instrument(SomethingAsync.class);
        final ISomethingAsync a = (ISomethingAsync) newClass.newInstance();

        CompletableFuture<String> blocker = CompletableFuture.completedFuture("x");
        final CompletableFuture<Object> res = a.doSomething(blocker);
        assertEquals(":x", res.join());
    }

    @Test
    public void testBlocking() throws IllegalAccessException, InstantiationException
    {
        InstrumentAsync ins = new InstrumentAsync();
        Class<?> newClass = ins.instrument(SomethingAsync.class);
        final ISomethingAsync a = (ISomethingAsync) newClass.newInstance();

        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletableFuture<Object> res = a.doSomething(blocker);
        blocker.complete("x");
        assertEquals(":x", res.join());
    }


    @Test
    public void testBlockingWithStackAndLocal() throws IllegalAccessException, InstantiationException
    {
        InstrumentAsync ins = new InstrumentAsync();
        Class<?> newClass = ins.instrument(SomethingWithLocalsAndStack.class);
        final ISomethingAsync a = (ISomethingAsync) newClass.newInstance();

        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletableFuture<Object> res = a.doSomething(blocker);
        blocker.complete("0123456789");
        assertEquals(":10", res.join());
    }

    @Test
    @org.junit.Ignore
    public void testBlockingAndException() throws IllegalAccessException, InstantiationException
    {
        InstrumentAsync ins = new InstrumentAsync();
        Class<?> newClass = ins.instrument(SomethingAsyncWithException.class);
        final ISomethingAsync a = (ISomethingAsync) newClass.newInstance();

        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletableFuture<Object> res = a.doSomething(blocker);
        blocker.completeExceptionally(new RuntimeException("Exception"));
        assertEquals(":Exception", res.join());
    }
}
