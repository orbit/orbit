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

package com.ea.orbit.actors.test.streams;

import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamMessageDispatcher;
import com.ea.orbit.actors.streams.StreamMessageHandler;
import com.ea.orbit.actors.streams.StreamSequenceToken;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class StreamMessageDispatcherTest extends ActorBaseTest
{
    private CompletableFuture<Test1> push1;
    private CompletableFuture<Test2> push2;

    @Before
    public void setUp() throws Exception
    {
        push1 = new Task<>();
        push2 = new Task<>();
    }

    @SuppressWarnings("Duplicates")
    @Test(timeout = 30_000L)
    public void testStreamMessageDispatcher()
    {
        createStage();
        StreamMessageDispatcher messageDispatcher = new StreamMessageDispatcher(Collections.singleton(this));
        try
        {
            AsyncStream<Object> test = AsyncStream.getStream(Object.class, "test");
            test.subscribe((d, t) -> {
                messageDispatcher.dispatchMessage(d, t);
                return Task.done();
            }).join();

            Test1 message1 = new Test1(42);
            test.publish(message1);
            assertEquals(message1.id, push1.join().id);

            Test2 message2 = new Test2(42);
            test.publish(message2);
            assertEquals(message2.id, push2.join().id);

            dumpMessages();
        }
        finally
        {
            messageDispatcher.destroy();
        }
    }

    @StreamMessageHandler
    public void handleTest1(Test1 test1, StreamSequenceToken sequenceToken)
    {
        push1.complete(test1);
    }

    @StreamMessageHandler
    public void handleTest2(Test2 test2)
    {
        push2.complete(test2);
    }

    private static final class Test1 implements Serializable
    {
        private final int id;

        public Test1(final int id)
        {
            this.id = id;
        }
    }

    private static final class Test2 implements Serializable
    {
        private final int id;

        public Test2(final int id)
        {
            this.id = id;
        }
    }
}
