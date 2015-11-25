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

package com.ea.orbit.actors.ws.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.client.ClientPeer;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.actors.test.FakeClusterPeer;
import com.ea.orbit.actors.test.FakeStorageExtension;
import com.ea.orbit.actors.transactions.IdUtils;
import com.ea.orbit.actors.ws.WebSocketClient;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Container;
import com.ea.orbit.web.EmbeddedHttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Singleton;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class StreamsWebSocketsTest extends ActorBaseTest
{
    private URI endpointURI;
    private Container container;

    public interface HelloWebApi
    {
        @Path("/hello")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        String hello(String message);
    }

    public static class HelloWebHandler implements HelloWebApi
    {
        @Override
        public String hello(String message)
        {
            return "helloWeb: " + message;
        }
    }

    public interface Hello extends Actor
    {
        Task<String> hello(String msg);

        Task<Void> doPush(String streamId, final String message);

    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<Void> doPush(final String streamId, final String message)
        {
            return AsyncStream.getStream(String.class, streamId)
                    .post(message);
        }

        public Task<String> hello(String msg)
        {
            return Task.fromValue("hello: " + msg);
        }
    }


    @Singleton
    public static class SFakePeer extends FakeClusterPeer
    {

    }

    @Before
    public void setup() throws URISyntaxException
    {
        // initializing the server
        Map<String, Object> properties = new HashMap<>();

        properties.put("orbit.http.port", 0);
        properties.put("orbit.http.metricsEnabled", false);
        properties.put("orbit.actors.clusterName", "cluster" + IdUtils.sequentialLongId());
        properties.put("orbit.components",
                Arrays.asList(
                        com.ea.orbit.actors.server.ServerModule.class,
                        SFakePeer.class,
                        HelloActor.class,
                        Hello.class,
                        HelloWebHandler.class,
                        EmbeddedHttpServer.class,
                        MyWebSocketServer.class));
        properties.put("orbit.actors.extensions",
                Arrays.asList(
                        new FakeStorageExtension(new ConcurrentHashMap<>()),
                        loggerExtension));

        container = new Container();
        container.setProperties(properties);
        container.start();
        final int localPort = container.get(EmbeddedHttpServer.class).getLocalPort();
        endpointURI = new URI("ws://localhost:" + localPort + "/websocket/con");
    }

    @After
    public void tearDown()
    {
        container.stop();
    }

    @Test(timeout = 30_000L)
    public void test()
    {
        final WebSocketClient clientEndPoint = new WebSocketClient();
        clientEndPoint.connect(endpointURI).join();
        final Hello hello = clientEndPoint.getReference(Hello.class, "0");

        assertEquals("hello: test", hello.hello("test").join());
        clientEndPoint.close();
    }

    @Test(timeout = 30_000L)
    public void testStreams() throws InterruptedException
    {
        final WebSocketClient client = new WebSocketClient();
        client.getPeer().addExtension(loggerExtension);
        client.connect(endpointURI).join();
        Hello hello = client.getReference(Hello.class, "0");
        hello.doPush("testStream", "hello").join();

        BlockingQueue<String> queue = new LinkedBlockingDeque<>();

        AsyncStream<String> testStream = client.getPeer().getStream(AsyncStream.DEFAULT_PROVIDER, String.class, "testStream");
        testStream.subscribe(d -> {
            queue.add(d);
            return Task.done();
        }).join();


        hello.doPush("testStream", "hello2").join();

        assertEquals("hello2", queue.poll(20, TimeUnit.SECONDS));
    }

}
