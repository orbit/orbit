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

import com.ea.orbit.async.Async;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class BasicInstrTest
{

    private static class Basic
    {
        static String concat(int i, long j, float f, double d, Object obj, boolean b)
        {
            return i + ":" + j + ":" + f + ":" + d + ":" + obj + ":" + b;
        }

        @Async
        public CompletableFuture<String> doSomething(CompletableFuture<String> blocker, int var)
        {
            return CompletableFuture.completedFuture(concat(var, 10_000_000_000L, 1.5f, 3.5d, blocker.join(), true));
        }
    }

    @org.junit.Ignore
    @Test
    public void testInstrumentation() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletableFuture<String> res = new Basic().doSomething(blocker, 5);
        blocker.complete("zzz");
        assertEquals("5:10000000000:1.5:3.5:zzz:true", res.join());
    }
}
