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
import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.actors.test.FakeClusterPeer;
import com.ea.orbit.actors.ws.server.ActorWebSocket;
import com.ea.orbit.actors.ws.server.Message;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Container;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.web.EmbeddedHttpServer;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class WsTest
{

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
            System.out.println("in hello web");
            return "helloWeb: " + message;
        }
    }

    public interface Hello extends Actor
    {
        Task<String> hello(String msg);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        public Task<String> hello(String msg)
        {
            System.out.println("in hello");
            return Task.fromValue("hello: " + msg);
        }
    }

    @ServerEndpoint("/ws/test")
    public static class MyActorWebSocket extends ActorWebSocket
    {

        private Session wsSession;

        @Override
        public void onOpen(final Session session)
        {
            wsSession = session;
            System.out.println("server open");
            super.onOpen(session);
        }

        @OnMessage
        public void onMessage(byte[] message, boolean last, Session session)
        {
            try
            {
                System.out.println("received");
                final Message message1 = mapper.readValue(message, Message.class);
                final Object payload = message1.getPayload();
                System.out.println(payload);

                switch (message1.getMessageType())
                {
                    case 1:
                        final Class<? extends Actor> clazz;
                        try
                        {
                            clazz = (Class<? extends Actor>) Class.forName((String) message1.getHeaders().get("class"));
                            String methodName = (String) message1.getHeaders().get("method");
                            final Method method = Stream.of(clazz.getMethods()).filter(m -> m.getName().equals(methodName)).findFirst().get();
                            Object[] args0 = payload instanceof List ? ((List) payload).toArray() : (Object[]) payload;
                            final Object[] args = castArgs(method.getGenericParameterTypes(), args0);
                            final Actor reference = Actor.getReference(clazz, "");
                            final Object res = method.invoke(reference, args);
                            final int messageId = message1.getMessageId();
                            if (res instanceof Task)
                            {
                                ((Task) res).handle((r, e) -> {
                                    final Message response = new Message();
                                    response.setMessageId(messageId);
                                    if (e == null)
                                    {
                                        response.setMessageType(2);
                                        response.setPayload(r);
                                    }
                                    else
                                    {
                                        response.setMessageType(3);
                                        response.setPayload(e);
                                    }
                                    final ByteBuffer wrap = ByteBuffer.wrap(serialize(response));
                                    wsSession.getAsyncRemote().sendBinary(wrap);
                                    return null;
                                });
                            }

                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                }
            }
            catch (IOException e)
            {
                throw new UncheckedException(e);
            }

        }

        private Object[] castArgs(final Type[] genericParameterTypes, final Object[] args0)
        {
            Object[] casted = new Object[genericParameterTypes.length];
            for (int i = 0; i < genericParameterTypes.length; i++)
            {
                casted[i] = mapper.convertValue(args0[0], mapper.getTypeFactory().constructType(genericParameterTypes[i]));
            }
            return casted;
        }

    }

    private static byte[] serialize(final Message message)
    {
        try
        {
            final String msg = mapper.writeValueAsString(message);
            System.out.println("serialized: " + msg);
            return msg.getBytes(StandardCharsets.UTF_8);
        }
        catch (JsonProcessingException e)
        {
            throw new UncheckedException(e);
        }
    }

    static ObjectMapper mapper = new ObjectMapper();

    static
    {
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }


    @ClientEndpoint
    public static class MyActorWebSocketClient
    {
        private Session wsSession;
        private AtomicInteger messageIdSeed = new AtomicInteger();

        private final Map<Integer, PendingResponse> pendingResponseMap = new ConcurrentHashMap<>();
        private final PriorityBlockingQueue<PendingResponse> pendingResponsesQueue = new PriorityBlockingQueue<>(50, new PendingResponseComparator());

        @OnOpen
        public void onOpen(Session wsSession)
        {
            this.wsSession = wsSession;
            System.out.println("opening websocket");
        }

        @OnClose
        public void onClose(Session userSession, CloseReason reason)
        {
            System.out.println("closing websocket");
        }

        @OnMessage
        public void onMessage(byte[] message, boolean last, Session session)
        {
            try
            {
                System.out.println("client received");
                final Message message1;
                message1 = mapper.readValue(message, Message.class);
                final Object payload = message1.getPayload();
                System.out.println(payload);
                switch (message1.getMessageType())
                {
                    case 2:
                        final PendingResponse pend = pendingResponseMap.get(message1.getMessageId());
                        if (pend != null)
                        {
                            pend.internalComplete(message1.getPayload());
                        }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        static class PendingResponse extends Task<Object>
        {
            final int messageId;
            final long timeoutAt;

            public PendingResponse(final int messageId, final long timeoutAt)
            {
                this.messageId = messageId;
                this.timeoutAt = timeoutAt;
            }

            @Override
            protected boolean internalComplete(Object value)
            {
                return super.internalComplete(value);
            }

            @Override
            protected boolean internalCompleteExceptionally(Throwable ex)
            {
                return super.internalCompleteExceptionally(ex);
            }
        }

        static class PendingResponseComparator implements Comparator<PendingResponse>
        {
            @Override
            public int compare(final PendingResponse o1, final PendingResponse o2)
            {
                int cmp = Long.compare(o1.timeoutAt, o2.timeoutAt);
                if (cmp == 0)
                {
                    return o1.messageId - o2.messageId;
                }
                return cmp;
            }
        }

        public <T> T getReference(Class<T> ref)
        {
            final Object o = Proxy.newProxyInstance(ref.getClassLoader(), new Class[]{ref},
                    (proxy, method, args) ->
                    {
                        Message message = new Message();
                        int messageId = messageIdSeed.incrementAndGet();
                        message.setMessageType(1);
                        message.setPayload(args);
                        message.setHeaders(new HashMap<>());
                        message.getHeaders().put("class", ref.getName());
                        message.getHeaders().put("method", method.getName());
                        message.setMessageId(messageId);
                        final byte[] bytes = serialize(message);
                        final Clock clock = Clock.systemUTC();
                        long timeoutAt = clock.millis() + 30_000;
                        PendingResponse pendingResponse = new PendingResponse(messageId, timeoutAt);
                        final boolean oneWay = method.isAnnotationPresent(OneWay.class);
                        if (!oneWay)
                        {
                            pendingResponseMap.put(messageId, pendingResponse);
                            pendingResponsesQueue.add(pendingResponse);
                        }
                        final ByteBuffer wrap = ByteBuffer.wrap(bytes);

                        //wrap.position(bytes.length);
                        final Future<Void> voidFuture = wsSession.getAsyncRemote().sendBinary(wrap);
                        voidFuture.get();

                        return pendingResponse;
                    });
            return (T) o;
        }


    }

    @Singleton
    public static class SFakePeer extends FakeClusterPeer
    {

    }

    @Test
    public void test() throws Exception
    {
        Map<String, Object> properties = new HashMap<>();

        properties.put("orbit.http.port", 0);
        properties.put("orbit.actors.clusterName", "cluster");
        properties.put("orbit.components", Arrays.asList(
                com.ea.orbit.actors.server.ServerModule.class,
                SFakePeer.class,
                HelloActor.class,
                Hello.class,
                HelloWebHandler.class,
                EmbeddedHttpServer.class,
                MyActorWebSocket.class));

        final Container container = new Container();
        container.setProperties(properties);
        container.start();

        final int localPort = container.get(EmbeddedHttpServer.class).getLocalPort();
        final WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();

        final URI endpointURI = new URI("ws://localhost:" + localPort + "/ws/test");
        final MyActorWebSocketClient clientEndPoint = new MyActorWebSocketClient();
        final Session session = wsContainer.connectToServer(clientEndPoint, endpointURI);
        final Hello hello = clientEndPoint.getReference(Hello.class);

        assertEquals("hello: test", hello.hello("test").join());
        session.close();
    }
}
