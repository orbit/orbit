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

package com.ea.orbit.web.test;

import com.ea.orbit.container.OrbitContainer;
import com.ea.orbit.util.NetUtils;

import org.junit.Test;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class WebSocketTest
{
    @ClientEndpoint
    public static class AClientEndpoint
    {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private Session session;

        @OnOpen
        public void onOpen(Session session)
        {
            this.session = session;
        }

        @OnMessage
        public void onMessage(String message)
        {
            messages.add(message);
        }
    }

    @Test
    public void test() throws URISyntaxException, IOException, DeploymentException, InterruptedException
    {
        final OrbitContainer container = new OrbitContainer();
        final int port = NetUtils.findFreePort();
        container.setProperties(Collections.singletonMap("orbit.http.port", String.valueOf(port)));
        container.start();

        final AClientEndpoint client = new AClientEndpoint();
        WebSocketContainer socketContainer = ContainerProvider.getWebSocketContainer();
        socketContainer.connectToServer(client, new URI("ws://localhost:" + port + "/echo"));

        // sends some messages to the server
        client.session.getBasicRemote().sendText("test1");
        client.session.getAsyncRemote().sendText("test2");

        // waits for the responses
        assertEquals("test1", client.messages.poll(10, TimeUnit.SECONDS));
        assertEquals("test2", client.messages.poll(10, TimeUnit.SECONDS));
        client.session.close();
        container.stop();

    }

}