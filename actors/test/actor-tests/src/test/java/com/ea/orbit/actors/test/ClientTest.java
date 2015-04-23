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

package com.ea.orbit.actors.test;


import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.cluster.INodeAddress;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.actors.test.actors.ISomeActor;
import com.ea.orbit.actors.test.actors.ISomeChatObserver;
import com.ea.orbit.actors.test.actors.ISomeChatRoom;
import com.ea.orbit.concurrent.Task;

import org.apache.commons.lang3.tuple.Pair;
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
    public static class SomeChatObserver implements ISomeChatObserver
    {
        BlockingQueue<Pair<ISomeChatObserver, String>> messagesReceived = new LinkedBlockingQueue<>();

        @Override
        public Task<Void> receiveMessage(final ISomeChatObserver sender, final String message)
        {
            messagesReceived.add(Pair.of(sender, message));
            return Task.done();
        }
    }


    @Test
    public void basicClientTest() throws ExecutionException, InterruptedException
    {
        OrbitStage stage = createStage();
        OrbitStage client = createClient();
        ISomeActor player = IActor.getReference(ISomeActor.class, "232");
        client.bind();
        assertEquals("bla", player.sayHello("meh").get());
    }

    @Test
    public void observerTest() throws ExecutionException, InterruptedException
    {
        OrbitStage stage1 = createStage();
        OrbitStage stage2 = createStage();
        OrbitStage client1 = createClient();
        OrbitStage client2 = createClient();

        SomeChatObserver observer1 = new SomeChatObserver();
        SomeChatObserver observer2 = new SomeChatObserver();
        {
            ISomeChatRoom chatRoom = IActor.getReference(ISomeChatRoom.class, "chat");
            client1.bind();
            chatRoom.join(observer1).get();
        }
        {
            client2.bind();
            ISomeChatRoom chatRoom = IActor.getReference(ISomeChatRoom.class, "chat");
            final ISomeChatObserver reference = client2.getObserverReference(observer2);
            chatRoom.join(reference).get();
            chatRoom.sendMessage(reference, "bla");
        }
        assertEquals("bla", observer1.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
        assertEquals("bla", observer2.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
    }

    @Test
    public void lonelyClientTest() throws ExecutionException, InterruptedException
    {
        OrbitStage client = createClient();
        ISomeActor player = IActor.getReference(ISomeActor.class, "232");
        client.getHosting().setTimeToWaitForServersMillis(100);
        expectException(() -> player.sayHello("meh"));
    }

	@Test
    public void ensureNoObjectsAreCreatedClientTest() throws ExecutionException, InterruptedException
    {
        List<OrbitStage> clients = new ArrayList<>();
        List<OrbitStage> servers = new ArrayList<>();
        Set<INodeAddress> serverAddresses = new HashSet<>();

        for (int i = 0; i < 20; i++)
        {
            clients.add(createClient());
        }

        for (int i = 0; i < 5; i++)
        {
            final OrbitStage server = createStage();
            servers.add(server);
            serverAddresses.add(server.getClusterPeer().localAddress());
        }
        for (int i = 0; i < 50; i++)
        {
            final OrbitStage client = clients.get((int) (Math.random() * clients.size()));
            ISomeActor player = IActor.getReference(ISomeActor.class, String.valueOf(i));
            client.bind();
            assertEquals("bla", player.sayHello("meh").join());
            assertTrue(serverAddresses.contains(client.getHosting().locateAndActivateActor((ActorReference<?>) player).join()));
        }

    }

}
