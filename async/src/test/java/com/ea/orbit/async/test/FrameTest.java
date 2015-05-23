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

import java.lang.reflect.InvocationTargetException;

import static com.ea.orbit.async.Await.await;
import static junit.framework.Assert.assertEquals;

public class FrameTest extends BaseTest
{
    @Test
    public void smallTest() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        // this test fails if using COMPUTE_FRAMES in the transformer;
        final Task<?> res = doIt(getBlockedTask(1));
        completeFutures();
        assertEquals(1, res.join());
    }


    private Task<Integer> doIt(final Task<Integer> t)
    {
        Number n = 1;
        if (n.intValue() == 1)
        {
            n = 1L;
        }
        else
        {
            n = 1.0;
        }
        return Task.fromValue(await(getBlockedTask(n.intValue())));
    }


    @Test
    public void bigTest() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        // this test fails if using COMPUTE_FRAMES in the transformer;
        final Task<?> res = big(getBlockedTask(1));
        completeFutures();
        assertEquals(1, res.join());
    }


    private Task<Integer> big(final Task<Integer> t)
    {
        Number n = 1;
        Integer i = 1;
        Float f = 1.0f;
        Double d = 1.0;
        Long l = 1L;

        switch (n.intValue())
        {
            case 1:
                n = i;
                await(t);
                n = f;
                break;
            case 2:
                n = f;
                await(t);
                n = d;
                break;
            case 3:
                n = d;
                await(t);
                n = l;
                break;
            case 4:
                n = l;
                await(t);
                n = i;
                break;
        }
        return Task.fromValue(await(getBlockedTask(n.intValue())));
    }

    @Test
    public void stackTest() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        // this test fails if using COMPUTE_FRAMES in the transformer;
        final Task<?> res = stack(getBlockedTask(1));
        completeFutures();
        assertEquals(1, res.join());
    }


    private Task<Integer> stack(final Task<Integer> t)
    {
        Number n = 1;
        if (t.isCancelled())
        {
            n = 1.0f;
        }
        // here the type of n is uncertain Float or Integer
        // the original frame will say Number

        // this fails if the Transform frame analysis can't get
        // the common superclass for Float and Integer
        // the best solution is to reuse the information from the original frames.
        multiparamFunction(n, await(t));
        return t;
    }

    void multiparamFunction(Number o1, Object o2)
    {
        // used just to have something in the stack
    }


//    @Test
//    public void uninitializedThisTest() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
//    {
//        // this test fails if not handling uninitialized objects
//        final Task<?> res = uninitializedThis(getBlockedTask(1));
//        completeFutures();
//        assertEquals(1, res.join());
//    }
//
//    private Task<Integer> uninitializedThis(final Task<Integer> t)
//    {
//        new SingleParamConstructor(await(t));
//        return t;
//    }
//
//    private static class SingleParamConstructor
//    {
//        SingleParamConstructor(Object o2)
//        {
//
//        }
//    }

    //    private Task<Integer> uninitializedThisMulti(final Task<Integer> t)
//    {
////        Number n = 1;
////        if (t.isCancelled())
////        {
////            n = 1.0f;
////        }
//        // here the type of n is uncertain Float or Integer
//        // the original frame will say Number
//
//        // this fails if the Transform frame analysis can't get
//        // the common superclass for Float and Integer
//        // the best solution is to reuse the information from the original frames.
//
//        // the test here is for the uninitialized "this" in the stack
//        new SingleParamConstructor(await(t));
//        return t;
//    }
//    private static class MultiParamConstructor
//    {
//        MultiParamConstructor(Number o1, Object o2)
//        {
//
//        }
//    }
}
