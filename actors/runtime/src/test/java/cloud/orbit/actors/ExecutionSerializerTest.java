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

package cloud.orbit.actors;

import cloud.orbit.actors.concurrent.WaitFreeExecutionSerializer;
import cloud.orbit.actors.concurrent.WaitFreeMultiExecutionSerializer;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import static org.jgroups.util.Util.assertEquals;

public class ExecutionSerializerTest
{
    @Test
    public void test()
    {
        WaitFreeExecutionSerializer executionSerializer = new WaitFreeExecutionSerializer(ForkJoinPool.commonPool());
        Task<String> hello = executionSerializer.executeSerialized(() -> Task.fromValue("hello"), 1000);
        assertEquals("hello", hello.join());
    }

    @Test(expected = RuntimeException.class)
    public void testException()
    {
        WaitFreeExecutionSerializer executionSerializer = new WaitFreeExecutionSerializer(ForkJoinPool.commonPool());
        Task<String> hello = executionSerializer.executeSerialized(() -> {
            throw new RuntimeException("Hello Exception");
        }, 1000);
        hello.join();
    }

    @Test
    public void testMulti()
    {
        WaitFreeMultiExecutionSerializer<String> executionSerializer = new WaitFreeMultiExecutionSerializer<>(ForkJoinPool.commonPool());
        Supplier<Task<String>> hello1 = () -> Task.fromValue("hello");
        Task<String> hello = executionSerializer.offerJob("aa", hello1, 1000);
        assertEquals("hello", hello.join());
    }

    @Test(expected = RuntimeException.class)
    public void testMultiException()
    {
        WaitFreeMultiExecutionSerializer<String> executionSerializer = new WaitFreeMultiExecutionSerializer<>(ForkJoinPool.commonPool());
        Supplier<Task<String>> taskSupplier = () -> {
            throw new RuntimeException("Hello Exception");
        };
        Task<String> hello = executionSerializer.offerJob("aa", taskSupplier, 1000);
        hello.join();
    }


}
