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

package cloud.orbit.actors.test.streams;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.client.ClientPeer;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.ea.async.Async.await;
import static org.junit.Assert.assertEquals;

public class StreamReferenceTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<AsyncStream<String>> getStreamReference();

        Task<Void> sayHello(final String greeting);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<AsyncStream<String>> getStreamReference()
        {
            return Task.fromValue(AsyncStream.getStream(String.class, "hello:" + getIdentity()));
        }

        @Override
        public Task<Void> sayHello(final String greeting)
        {
            return await(getStreamReference()).publish(greeting);
        }
    }

    @Test(timeout = 30_000L)
    public void testStreamReferenceWithRemoteClient() throws InterruptedException
    {
        final Stage stage1 = createStage();


        final ClientPeer client = createRemoteClient(stage1);

        BlockingDeque<Object> received = fakeSync.deque("received");

        client.bind();
        AsyncStream<String> testStream = client.getReference(Hello.class, "0").getStreamReference().join();
        testStream.subscribe((d, t) -> {
            received.add(d);
            return Task.done();
        }).join();


        stage1.bind();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.sayHello("hello2").join();

        assertEquals("hello2", received.poll(20, TimeUnit.SECONDS));
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void testStreamReferenceWithRemoteStage() throws InterruptedException
    {
        final Stage stage1 = createStage();
        Hello hello1 = Actor.getReference(Hello.class, "0");
        // ensure the actor gets instantiated in the first stage
        hello1.sayHello("hello1");

        final Stage stage2 = createStage();
        Hello hello2 = Actor.getReference(Hello.class, "0");


        BlockingDeque<Object> received = fakeSync.deque("received");

        AsyncStream<String> testStream = hello2.getStreamReference().join();

        testStream.subscribe((d, t) -> {
            received.add(d);
            return Task.done();
        }).join();


        stage1.bind();
        hello1.sayHello("hello2").join();

        assertEquals("hello2", received.poll(20, TimeUnit.SECONDS));
        dumpMessages();
    }
}
