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
import cloud.orbit.actors.annotation.PreferLocalPlacement;
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
import static org.jgroups.util.Util.assertEquals;

public class StreamPersistenceTest extends ActorBaseTest
{
    @PreferLocalPlacement
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
                        testSync.deque("queue").add(d);
                        return Task.done();
                    }));
            return Task.done();
        }
    }


    @Test(timeout = 30_000L)
    public void testStreamPersistence() throws InterruptedException
    {
        final Stage stage1 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        // this will force the stream to be activated in stage 1
        test.publish("hello").join();

        final Stage stage2 = createStage();
        // since hello is @PreferLocalPlacement it will be activated in stage2
        Hello hello2 = Actor.getReference(Hello.class, "2");
        hello2.doSubscribe("test").join();


        BlockingQueue<Object> queue = fakeSync.deque("queue");
        test.publish("hello2");
        // stream in stage 1, actor in stage 2
        assertEquals("hello2", queue.poll(10, TimeUnit.SECONDS));

        // now stopping stage1 to force the stream deactivation
        // the actor will remain alive in stage 2
        stage1.stop().join();

        // the stream will be activated again in stage 2
        test.publish("hello3");
        assertEquals("hello3", queue.poll(10, TimeUnit.SECONDS));


        dumpMessages();
    }

}
