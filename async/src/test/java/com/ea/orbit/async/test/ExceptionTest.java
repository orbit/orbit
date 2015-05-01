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
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExceptionTest extends BaseTest
{

    @Test
    public void testTryCatch() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        final Task<Integer> res = doTryCatch();
        completeFutures();
        assertEquals((Integer) 10, res.join());
    }

    @Async
    private Task<Integer> doTryCatch()
    {
        try
        {
            if (await(getBlockedFuture(10)) == 10)
            {
                throw new IllegalArgumentException(String.valueOf(10));
            }

        }
        catch (IllegalArgumentException ex)
        {
            return Task.fromValue(10);
        }
        return null;
    }


    @Test
    public void testTryCatch2() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        final Task res = doTryCatch2();
        completeFutures();
        assertTrue(res.isDone());
    }

    @Async
    private Task doTryCatch2()
    {
        int c = 1;
        try
        {
            await(getBlockedFuture());
        }
        catch (Exception ex)
        {
            // once this was causing a verification error, now fixed
            c = c + 1;
        }
        return Task.done();
    }


    @Test
    public void testTryCatch3() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        final Task res = doTryCatch3();
        completeFutures();
        assertEquals("fail", res.join());
    }

    @Async
    private Task doTryCatch3()
    {
        int c = 1;
        try
        {
            await(getBlockedTask());
            await(getBlockedTask());
            await(getBlockedTask());
            await(getBlockedTask());
            await(Task.fromException(new Exception("fail")));
        }
        catch (Exception ex)
        {
            await(getBlockedTask());
            await(getBlockedTask());
            await(getBlockedTask());
            await(getBlockedTask());
            return Task.fromValue(ex.getCause().getMessage());
        }
        return Task.done();
    }


    @Test
    public void testTryCatch4() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        final Task res = doTryCatch4();
        completeFutures();
        assertEquals("fail", res.join());
    }

    @Async
    private Task doTryCatch4()
    {
        int c = 1;
        try
        {
            await(getBlockedTask());
            await(getBlockedTask());
            if (await(getBlockedTask()) == null)
            {
                throw new Exception("fail");
            }
            await(getBlockedTask());
        }
        catch (Exception ex)
        {
            await(getBlockedTask());
            await(getBlockedTask());
            await(getBlockedTask());
            await(getBlockedTask());
            return Task.fromValue(ex.getMessage());
        }
        return Task.done();
    }
}
