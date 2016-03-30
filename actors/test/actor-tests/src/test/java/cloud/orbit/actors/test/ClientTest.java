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

package cloud.orbit.actors.test;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.actors.test.actors.SomeActor;
import cloud.orbit.actors.test.actors.SomeChatRoom;
import cloud.orbit.concurrent.Task;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class ClientTest extends ActorBaseTest
{
    public static class SomeChatObserver implements cloud.orbit.actors.test.actors.SomeChatObserver
    {
        BlockingQueue<Pair<cloud.orbit.actors.test.actors.SomeChatObserver, String>> messagesReceived = new LinkedBlockingQueue<>();

        @Override
        public Task<Void> receiveMessage(final cloud.orbit.actors.test.actors.SomeChatObserver sender, final String message)
        {
            messagesReceived.add(Pair.of(sender, message));
            return Task.done();
        }
    }


    @Test
    public void basicClientTest() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Stage client = createClient();
        SomeActor player = Actor.getReference(SomeActor.class, "232");
        client.bind();
        Assert.assertEquals("bla", player.sayHello("meh").get());
    }

    @Test(timeout = 10_000L)
    public void observerTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();
        Stage client1 = createClient();
        Stage client2 = createClient();

        SomeChatObserver observer1 = new SomeChatObserver();
        SomeChatObserver observer2 = new SomeChatObserver();
        {
            SomeChatRoom chatRoom = Actor.getReference(SomeChatRoom.class, "chat");
            client1.bind();
            chatRoom.join(observer1).get();
        }
        {
            client2.bind();
            SomeChatRoom chatRoom = Actor.getReference(SomeChatRoom.class, "chat");
            final cloud.orbit.actors.test.actors.SomeChatObserver reference = client2.registerObserver(null, observer2);
            chatRoom.join(reference).get();
            chatRoom.sendMessage(reference, "bla");
        }
        assertEquals("bla", observer1.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
        assertEquals("bla", observer2.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
    }

    @Test
    public void lonelyClientTest() throws ExecutionException, InterruptedException
    {
        Stage client = createClient();
        SomeActor player = Actor.getReference(SomeActor.class, "232");
        client.getHosting().setTimeToWaitForServersMillis(100);
        expectException(() -> player.sayHello("meh"));
    }

	@Test
    public void ensureNoObjectsAreCreatedClientTest() throws ExecutionException, InterruptedException
    {
        List<Stage> clients = new ArrayList<>();
        List<Stage> servers = new ArrayList<>();
        Set<NodeAddress> serverAddresses = new HashSet<>();

        for (int i = 0; i < 20; i++)
        {
            clients.add(createClient());
        }

        for (int i = 0; i < 5; i++)
        {
            final Stage server = createStage();
            servers.add(server);
            serverAddresses.add(server.getClusterPeer().localAddress());
        }
        for (int i = 0; i < 50; i++)
        {
            final Stage client = clients.get((int) (Math.random() * clients.size()));
            SomeActor player = Actor.getReference(SomeActor.class, String.valueOf(i));
            client.bind();
            Assert.assertEquals("bla", player.sayHello("meh").join());
            assertTrue(serverAddresses.contains(client.getHosting().locateActor((RemoteReference<?>) player, true).join()));
        }

    }

}
