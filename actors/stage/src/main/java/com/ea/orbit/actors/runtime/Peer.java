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

package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.NotImplementedException;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.io.ByteBufferInputStream;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This works as a bridge to perform calls between the server and a client.
 */
public class Peer
{
    private Runtime runtime;

    private MessageSerializer serializer;
    private AtomicInteger messageIdSeed = new AtomicInteger();

    private final Map<Integer, PendingResponse> pendingResponseMap = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<PendingResponse> pendingResponsesQueue = new PriorityBlockingQueue<>(50, new PendingResponseComparator());
    private Clock clock = Clock.systemUTC();

    public void setRuntime(Runtime runtime)
    {
        this.runtime = runtime;
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

    public void sendMessage(Message message)
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            serializer.serializeMessage(null, baos, message);
            final ByteBuffer wrap = ByteBuffer.wrap(baos.toByteArray());
            sendBinary(wrap);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new UncheckedException(e);
        }
    }

    protected void sendBinary(final ByteBuffer wrap)
    {
        throw new NotImplementedException("sendBinary");
    }

    final CompletableFuture<?> safeInvoke(Object target, Method method, Object[] params)
    {
        try
        {
            final Object res = method.invoke(target, params);
            if (res instanceof CompletableFuture)
            {
                return (CompletableFuture<?>) res;
            }

            return res != null ? Task.fromValue(res) : Task.done();
        }
        catch (Throwable ex)
        {
            return Task.fromException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public void onMessage(final ByteBuffer data)
    {
        try
        {
            final Message message = serializer.deserializeMessage(runtime, new ByteBufferInputStream(data));
            switch (message.getMessageType())
            {
                case MessageDefinitions.REQUEST_MESSAGE:
                case MessageDefinitions.ONE_WAY_MESSAGE:
                    try
                    {
                        final ActorInvoker invoker = runtime.getInvoker(message.getInterfaceId());

                        final Actor reference = Actor.getReference(invoker.getInterface(), "");
                        final int messageId = message.getMessageId();
                        final Object res = invoker.safeInvoke(reference, message.getMethodId(), (Object[]) message.getPayload());
                        if (message.getMessageType() == MessageDefinitions.REQUEST_MESSAGE)
                        {
                            if (res instanceof CompletableFuture)
                            {
                                ((CompletableFuture) res).handle((r, e) -> {
                                    handleResponse(messageId, r, (Throwable) e);
                                    return null;
                                });
                            }
                            else
                            {
                                handleResponse(messageId, res, null);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    break;

                case MessageDefinitions.RESPONSE_OK:
                case MessageDefinitions.RESPONSE_ERROR:
                case MessageDefinitions.RESPONSE_PROTOCOL_ERROR:
                    final PendingResponse pend = pendingResponseMap.get(message.getMessageId());
                    if (pend != null)
                    {
                        final Object payload = message.getPayload();
                        if (message.getMessageType() == MessageDefinitions.RESPONSE_OK)
                        {
                            pend.internalComplete(payload);
                        }
                        else
                        {
                            pend.internalCompleteExceptionally(
                                    payload instanceof Exception
                                            ? (Throwable) payload
                                            : new UncheckedException(String.valueOf(payload)));
                        }
                    }
                    break;
            }
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }


    private Object handleResponse(final int messageId, final Object r, final Throwable e)
    {
        final Message response = new Message();
        response.setMessageId(messageId);
        if (e == null)
        {
            response.setMessageType(MessageDefinitions.RESPONSE_OK);
            response.setPayload(r);
        }
        else
        {
            response.setMessageType(MessageDefinitions.RESPONSE_ERROR);
            response.setPayload(e);
        }
        sendMessage(response);
        return null;
    }

    public <T> T getReference(Class<T> ref, String objectId)
    {
        final Object o = Proxy.newProxyInstance(ref.getClassLoader(), new Class[]{ref},
                (proxy, method, args) ->
                {
                    Message message = new Message();
                    int messageId = messageIdSeed.incrementAndGet();
                    message.setMessageType(MessageDefinitions.REQUEST_MESSAGE);
                    message.setPayload(args);
                    message.setObjectId(objectId);
                    message.setInterfaceId(ref.getName().replace('$', '.').hashCode());
                    message.setMethodId(computeMethodId(method));
                    message.setMessageId(messageId);

                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    serializer.serializeMessage(runtime, out, message);
                    final byte[] bytes = out.toByteArray();
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
                    sendBinary(wrap);

                    return pendingResponse;
                });
        return (T) o;
    }

    private int computeMethodId(Method method)
    {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final String methodName = method.getName();
        final String methodSignature = methodName + "(" + Stream.of(parameterTypes).map(p -> p.getName()).collect(Collectors.joining(",")) + ")";
        return methodSignature.hashCode();
    }


    public void setSerializer(final MessageSerializer serializer)
    {
        this.serializer = serializer;
    }

    public void onClose()
    {
        // TODO: fail pending messages
    }

    public void onOpen()
    {

    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }
}
