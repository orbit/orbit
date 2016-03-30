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
import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.OneWay;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.RemoteClient;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.concurrent.Task;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class ClientLocalObjectsTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<String> sayHello(String greeting);
    }

    public interface Notification
    {

    }

    public interface NotificationListener extends ActorObserver
    {
        @OneWay
        Task<Void> receiveNotification(Notification notification);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<String> sayHello(final String greeting)
        {
            return Task.fromValue(greeting);
        }
    }

    @Test(timeout = 60_000L)
    @Ignore
    public void callServer() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Actor.getReference(Hello.class, "1").sayHello("hello").join();

        RemoteClient client = createRemoteClient(stage);
        Hello actor1 = client.getReference(Hello.class, "1000");

        BlockingQueue<Notification> received = new LinkedBlockingQueue<>();
        client.registerObserver(NotificationListener.class, n -> {
            received.add(n);
            return Task.done();
        });

        assertNotNull(received.poll(30, TimeUnit.SECONDS));
    }


}
