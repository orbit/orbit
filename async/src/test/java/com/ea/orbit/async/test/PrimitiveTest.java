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
import com.ea.orbit.async.Await;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PrimitiveTest
{
    static
    {
        Await.init();
    }

    public static abstract class Base
    {
        int i1;
        short s1;
        byte b1;
        double d1;
        char c1;
        boolean z1;
        long g1;
        Object o1;
        float f1;
        int i2;

        public CompletableFuture<Object> intTest(CompletableFuture<String> blocker, int var)
        {
            return null;
        }

        public CompletableFuture<Object> longTest(CompletableFuture<String> blocker, long var)
        {
            return null;
        }

        public CompletableFuture<Object> melange(
                int pi1,
                short ps1,
                byte pb1,
                double pd1,
                char pc1,
                boolean pz1,
                long lg1,
                Object po1,
                float pf1,
                CompletableFuture<String> blocker, long var)
        {
            return null;
        }

        public void setFields(int pi1, short ps1, byte pb1, double pd1, char pc1,
                              boolean pz1, long pg1, Object po1, float pf1, int pi2)
        {
            this.i1 = pi1;
            this.s1 = ps1;
            this.b1 = pb1;
            this.d1 = pd1;
            this.c1 = pc1;
            this.z1 = pz1;
            this.g1 = pg1;
            this.o1 = po1;
            this.f1 = pf1;
            this.i2 = pi2;
        }

        public void assertFields(int pi1, short ps1, byte pb1, double pd1, char pc1,
                                 boolean pz1, long pg1, Object po1, float pf1)
        {
            assertEquals(pi1, i1);
            assertEquals(ps1, s1);
            assertEquals(pb1, b1);
            assertEquals(pd1, d1, 0);
            assertEquals(pc1, c1);
            assertEquals(pz1, z1);
            assertEquals(pg1, g1);
            assertEquals(po1, o1);
            assertEquals(pf1, f1, 0);
        }
    }

    public static class PrimitiveUser1 extends Base
    {
        @Async
        public CompletableFuture<Object> intTest(CompletableFuture<String> blocker, int var)
        {
            String res = await(blocker);
            return CompletableFuture.completedFuture(":" + var + ":" + res);
        }
    }

    public static class PrimitiveUser2 extends Base
    {
        @Async
        public CompletableFuture<Object> longTest(CompletableFuture<String> blocker, long var)
        {
            String res = await(blocker);
            return CompletableFuture.completedFuture(":" + var + ":" + res);
        }
    }

    public static class PrimitiveUser3 extends Base
    {
        @Async
        public CompletableFuture<Object> melange(
                int pi1,
                short ps1,
                byte pb1,
                double pd1,
                char pc1,
                boolean pz1,
                long lg1,
                Object po1,
                float pf1,
                CompletableFuture<String> blocker, long var)
        {
            await(blocker);
            setFields(pi1, ps1, pb1, pd1, pc1, pz1, lg1, po1, pf1, 0);
            return CompletableFuture.completedFuture(":");
        }
    }

    public static class PrimitiveUser4 extends Base
    {
        @Async
        public CompletableFuture<Object> melange(
                int pi1,
                short ps1,
                byte pb1,
                double pd1,
                char pc1,
                boolean pz1,
                long lg1,
                Object po1,
                float pf1,
                CompletableFuture<String> blocker, long var)
        {
            // blocks with a lot of primitives in the stack
            this.setFields(pi1, ps1, pb1, pd1, pc1, pz1, lg1, po1, pf1, await(blocker) != null ? 0 : 1);
            return CompletableFuture.completedFuture(":");
        }
    }

    @Test
    public void testIntParam() throws IllegalAccessException, InstantiationException
    {
        PrimitiveUser1 a = new PrimitiveUser1();

        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletableFuture<Object> res = a.intTest(blocker, 11);
        blocker.complete("x");
        assertEquals(":11:x", res.join());
    }

    @Test
    public void testLongParam() throws IllegalAccessException, InstantiationException
    {
        PrimitiveUser2 a = new PrimitiveUser2();

        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletableFuture<Object> res = a.longTest(blocker, 10000000000L);
        blocker.complete("x");
        assertEquals(":10000000000:x", res.join());
    }

    @Test
    public void testAllPrimitivesAsLocalVars() throws IllegalAccessException, InstantiationException
    {
        PrimitiveUser3 a = new PrimitiveUser3();

        CompletableFuture<String> blocker = new CompletableFuture<>();

        Random r = new Random();
        int i1 = r.nextInt();
        short s1 = (short) r.nextInt();
        byte b1 = (byte) r.nextInt();
        double d1 = r.nextDouble();
        char c1 = (char) r.nextInt();
        boolean z1 = r.nextBoolean();
        long g1 = r.nextLong();
        Object o1 = new Object();
        float f1 = r.nextFloat();

        final CompletableFuture<Object> res = a.melange(i1, s1, b1, d1, c1, z1, g1, o1, f1, blocker, 0);
        assertFalse(res.isDone());
        blocker.complete("x");
        assertEquals(":", res.join());
        a.assertFields(i1, s1, b1, d1, c1, z1, g1, o1, f1);
    }

    @Test
    public void testAllPrimitivesAndStack() throws IllegalAccessException, InstantiationException
    {
        PrimitiveUser4 a = new PrimitiveUser4();

        CompletableFuture<String> blocker = new CompletableFuture<>();

        Random r = new Random();
        int i1 = r.nextInt();
        short s1 = (short) r.nextInt();
        byte b1 = (byte) r.nextInt();
        double d1 = r.nextDouble();
        char c1 = (char) r.nextInt();
        boolean z1 = r.nextBoolean();
        long g1 = r.nextLong();
        Object o1 = new Object();
        float f1 = r.nextFloat();

        final CompletableFuture<Object> res = a.melange(i1, s1, b1, d1, c1, z1, g1, o1, f1, blocker, 0);
        assertFalse(res.isDone());
        blocker.complete("x");
        assertEquals(":", res.join());
        a.assertFields(i1, s1, b1, d1, c1, z1, g1, o1, f1);
    }
}
