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
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.actors.test.FakeSync;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import static com.ea.async.Async.await;
import static org.junit.Assert.*;

public class StreamWithActorsTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<Void> doSubscribe(String streamId);

        Task<Void> doUnSubscribe(final String streamId);
    }

    public static class HelloActor extends AbstractActor<HelloActor.State> implements Hello
    {
        @Inject
        FakeSync testSync;
        private StreamSubscriptionHandle<String> handle;

        public static class State
        {
            String last;
        }

        @Override
        public Task<Void> doSubscribe(final String streamId)
        {
            getLogger().info("doSubscribe hash:" + hashCode());
            handle = await(AsyncStream.getStream(String.class, streamId)
                    .subscribe((d,t) -> {
                        state().last = d;
                        // test framework trick
                        testSync.put(getIdentity() + "-last", d);
                        return Task.done();
                    }));
            return Task.done();
        }


        @Override
        public Task<Void> doUnSubscribe(final String streamId)
        {
            getLogger().info("doUnSubscribe hash:" + hashCode());
            await(AsyncStream.getStream(String.class, streamId).unsubscribe(handle));
            return Task.done();
        }

        @Override
        public Task<?> activateAsync()
        {
            getLogger().info("activateAsync");
            return super.activateAsync();
        }
    }

    @Test(timeout = 30_000L)
    public void test()
    {
        createStage();
        Actor.getReference(Hello.class, "0").doSubscribe("test").join();

        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello");
        assertEquals("hello", fakeSync.task("0-last").join());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void test2stages()
    {
        final Stage stage1 = createStage();
        Actor.getReference(Hello.class, "0").doSubscribe("test").join();

        final Stage stage2 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello");
        assertEquals("hello", fakeSync.task("0-last").join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void test2stagesWithDeliveryNotification()
    {
        final Stage stage1 = createStage();
        Actor.getReference(Hello.class, "0").doSubscribe("test").join();

        final Stage stage2 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello").join();
        assertTrue(fakeSync.task("0-last").isDone());
        assertEquals("hello", fakeSync.get("0-last").join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void testUnSubscribe()
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.doSubscribe("test").join();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        hello.doUnSubscribe("test").join();
        assertFalse(fakeSync.task("0-last").isDone());
        test.publish("hello").join();
        assertFalse(fakeSync.task("0-last").isDone());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void testUnSubscribe2()
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.doSubscribe("test").join();

        final Stage stage2 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");

        hello.doUnSubscribe("test").join();
        assertFalse(fakeSync.task("0-last").isDone());
        test.publish("hello").join();
        assertFalse(fakeSync.task("0-last").isDone());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void test2SubscriberActors()
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "1");
        hello.doSubscribe("test").join();

        final Stage stage2 = createStage();
        Hello hello2 = Actor.getReference(Hello.class, "2");
        hello2.doSubscribe("test").join();


        final Stage stage3 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");

        test.publish("hello").join();

        assertTrue(fakeSync.task("1-last").isDone());
        assertTrue(fakeSync.task("2-last").isDone());
        dumpMessages();
    }

    // TODO test stage crash (forceful removal from the network)

}
