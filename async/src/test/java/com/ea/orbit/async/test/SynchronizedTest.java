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

import com.ea.orbit.concurrent.Task;

import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static com.ea.orbit.async.Await.await;
import static com.ea.orbit.concurrent.Task.fromValue;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SynchronizedTest extends BaseTest
{
    @Test
    public void outOfTheWay()
    {
        class Experiment
        {
            int x;

            Task doIt(Object mutex, int a)
            {
                synchronized (mutex)
                {
                    x = 1;
                }
                await(getBlockedTask());
                return fromValue(x + a);
            }
        }
        final Task res = new Experiment().doIt(new Object(), 1);
        completeFutures();
        assertEquals(2, res.join());
    }

    @Test
    @Ignore
    // todo fix this
    public void inThePath()
    {
        class Experiment
        {
            int x;

            Task doIt(Object mutex, int a)
            {
                synchronized (mutex)
                {
                    x = 1;
                    await(getBlockedTask());
                }
                return fromValue(x + a);
            }
        }
        final Task res = new Experiment().doIt(new Object(), 1);
        completeFutures();
        assertEquals(2, res.join());
    }

    @Test
    @Ignore
    // todo fix this
    public void twoMutexes()
    {
        class Experiment
        {
            int x;

            Task doIt(Object mutex1, Object mutex2, int a)
            {
                synchronized (mutex1)
                {
                    synchronized (mutex2)
                    {
                        x = 1;
                        await(getBlockedTask());
                    }
                }
                return fromValue(x + a);
            }
        }
        final Task res = new Experiment().doIt("a", "b", 1);
        completeFutures();
        assertEquals(2, res.join());
    }

    @Test
    @Ignore
    // todo fix this
    public void usingThis()
    {
        class Experiment
        {
            int x;

            Task doIt(int a)
            {
                synchronized (this)
                {
                    x = 1;
                    await(getBlockedTask());
                }
                return fromValue(x + a);
            }
        }
        final Task res = new Experiment().doIt(0);
        completeFutures();
        assertEquals(1, res.join());
    }

    @Test
    @Ignore
    // todo fix this
    public void synchronizedMethod()
    {
        class Experiment
        {
            int x;

            synchronized Task doIt(int a)
            {
                x = 1;
                await(getBlockedTask());
                return fromValue(x);
            }
        }
        final Task res = new Experiment().doIt(0);
        Method asyncMethod = Stream.of(Experiment.class.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("async$"))
                .findFirst().orElse(null);
        completeFutures();
        assertEquals(1, res.join());
        assertTrue(Modifier.isSynchronized(asyncMethod.getModifiers()));
    }
}
