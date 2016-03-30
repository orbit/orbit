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

package cloud.orbit.actors.test.client;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.RemoteClient;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.actors.test.FakeSync;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class RemoteClientTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<String> sayHello(String greeting);

        Task<String> releaseThenAcquire(final String acquire, final String release);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Inject
        FakeSync sync;

        @Override
        public Task<String> sayHello(final String greeting)
        {
            return Task.fromValue(greeting);
        }

        @Override
        public Task<String> releaseThenAcquire(final String release, final String acquire)
        {
            // lets the client go
            sync.semaphore(release).release();
            // blocks waiting for the other side
            sync.semaphore(acquire).acquire(10, TimeUnit.SECONDS);
            return Task.fromValue(release + acquire);
        }
    }

    @Test
    public void callServer() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        RemoteClient client = createRemoteClient(stage);
        Hello actor1 = client.getReference(Hello.class, "1000");
        assertEquals("test", actor1.sayHello("test").join());
        dumpMessages();
    }


    @Test
    public void timeoutTest() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        clock.stop();
        // make sure the actor is there... remove this later
        RemoteClient client = createRemoteClient(stage);
        Hello someActor = Actor.getReference(Hello.class, "1");
        Task<String> res = someActor.releaseThenAcquire("aa", "bb");

        // blocks until the server gets the message
        fakeSync.semaphore("aa").acquireUninterruptibly();

        // forward in time
        clock.incrementTime(60, TimeUnit.MINUTES);

        // run client message cleanup;
        client.cleanup().join();
        expectException(() -> res.get(10, TimeUnit.SECONDS));
        assertTrue(res.isDone());
        assertTrue(res.isCompletedExceptionally());

        // releases the server
        fakeSync.semaphore("bb").release();

        dumpMessages();
    }

}
