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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.ea.async.Async.await;
import static org.junit.Assert.assertEquals;

public class StreamActorDeactivationTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<Void> doSubscribe(String streamId);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Inject
        FakeSync testSync;

        private StreamSubscriptionHandle<String> handle;


        @Override
        public Task<Void> doSubscribe(final String streamId)
        {
            getLogger().info("doSubscribe");
            handle = await(AsyncStream.getStream(String.class, streamId)
                    .subscribe((d, t) -> {
                        // test framework trick
                        testSync.deque(getIdentity() + "-last").add(d);
                        return Task.done();
                    }));
            return Task.done();
        }
    }

    @Test(timeout = 30_000L)
    public void testDeactivate() throws InterruptedException
    {
        final Stage stage1 = createStage();
        clock.stop();
        Hello hello = Actor.getReference(Hello.class, "1");
        hello.doSubscribe("test").join();

        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello").join();
        BlockingQueue<Object> queue = fakeSync.deque("1-last");

        assertEquals("hello", queue.poll(10, TimeUnit.SECONDS));
        clock.incrementTime(20, TimeUnit.MINUTES);
        // actors get deactivated
        stage1.cleanup().join();

        // the actor has been deactivated so it won't receive this message
        test.publish("hello2").join();
        // subscribing again
        hello.doSubscribe("test").join();

        test.publish("hello3").join();
        // the message hello2 can't have being processed.
        assertEquals("hello3", queue.poll(10, TimeUnit.SECONDS));

        dumpMessages();
    }

}
