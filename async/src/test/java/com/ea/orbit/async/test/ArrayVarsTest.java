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

import org.junit.Test;

import static com.ea.orbit.async.Await.await;
import static com.ea.orbit.concurrent.Task.fromValue;
import static junit.framework.Assert.assertEquals;

public class ArrayVarsTest extends BaseTest
{
    @Test
    public void arrayParams01()
    {
        class Experiment
        {
            Task doIt(Object params[])
            {
                await(Task.done());
                return fromValue(params[0]);
            }
        }
        assertEquals("x", new Experiment().doIt(new Object[]{"x"}).join());
    }

    @Test
    public void arrayParams02()
    {
        class Experiment
        {
            Task doIt(Object params[][])
            {
                await(Task.done());
                return fromValue(params[0][0]);
            }
        }
        assertEquals("x", new Experiment().doIt(new Object[][]{{"x"}}).join());
    }

    @Test
    public void arrayParams03()
    {
        class Experiment
        {
            Task doIt(Object params[][][])
            {
                await(Task.done());
                return fromValue(params[0][0][0]);
            }
        }
        assertEquals("x", new Experiment().doIt(new Object[][][]{{{"x"}}}).join());
    }

    @Test
    public void arrayParams04()
    {
        class Experiment
        {
            Task doIt(Object params[][][][])
            {
                await(Task.done());
                return fromValue(params[0][0][0][0]);
            }
        }
        assertEquals("x", new Experiment().doIt(new Object[][][][]{{{{"x"}}}}).join());
    }


    @Test
    public void optionalParam()
    {
        class Experiment
        {
            Task doIt(Object... params)
            {
                await(Task.done());
                return fromValue(params[0]);
            }
        }
        assertEquals("x", new Experiment().doIt(new Object[]{"x"}).join());
    }


    @Test
    public void stringArrayParams01()
    {
        class Experiment
        {
            Task doIt(String params[])
            {
                await(Task.done());
                return fromValue(params[0]);
            }
        }
        assertEquals("x", new Experiment().doIt(new String[]{"x"}).join());
    }

    @Test
    public void arrayVar01()
    {
        class Experiment
        {
            Task doIt(String x)
            {
                Object arr[] = new Object[]{x};
                await(Task.done());
                return fromValue(arr[0]);
            }
        }
        assertEquals("x", new Experiment().doIt("x").join());
    }


    @Test
    public void stringVar01()
    {
        class Experiment
        {
            Task doIt(String x)
            {
                String arr[] = new String[]{x};
                await(Task.done());
                return fromValue(arr[0]);
            }
        }
        assertEquals("x", new Experiment().doIt("x").join());
    }

    @Test
    public void primitiveArray01()
    {
        class Experiment
        {
            Task doIt(int x)
            {
                int arr[] = new int[]{x};
                await(Task.done());
                return fromValue(arr[0]);
            }
        }
        assertEquals(10, new Experiment().doIt(10).join());
    }

    @Test
    public void arraysWithAwait01()
    {
        class Experiment
        {
            Task doIt(Object params[])
            {
                await(getBlockedFuture());
                return fromValue(params[0]);
            }
        }
        final Task task = new Experiment().doIt(new Object[]{"x"});
        completeFutures();
        assertEquals("x", task.join());
    }

    @Test
    public void arraysWithAwait02()
    {
        class Experiment
        {
            Task doIt(Object params[])
            {
                Object arr[][] = new Object[][]{params};
                await(getBlockedFuture());
                return fromValue(arr[0][0]);
            }
        }
        final Task task = new Experiment().doIt(new Object[]{"x"});
        completeFutures();
        assertEquals("x", task.join());
    }


    @Test
    public void arraysWithAwait03()
    {
        class Experiment
        {
            Task doIt(Object params[])
            {
                Object arr[][];
                if (params != null)
                {
                    arr = new Object[][]{params};
                    await(getBlockedFuture());
                }
                else
                {
                    arr = new Object[][]{params, null};
                    await(getBlockedFuture());
                }
                return fromValue(arr[0][0]);
            }
        }
        final Task task = new Experiment().doIt(new Object[]{"x"});
        completeFutures();
        assertEquals("x", task.join());
    }

    @Test
    public void arraysAndIfs()
    {
        class Experiment
        {
            Task doIt(int x)
            {
                Object[][] arr = new Object[][]{{x}};
                if (x == 11)
                {
                    arr = null;
                }
                // this forces a stack frame map to be created
                else
                {

                    await(getBlockedFuture());
                }
                return fromValue(arr[0][0]);
            }
        }
        final Task task = new Experiment().doIt(10);
        completeFutures();
        assertEquals(10, task.join());
    }
}
