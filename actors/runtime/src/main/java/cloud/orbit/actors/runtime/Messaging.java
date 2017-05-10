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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.Addressable;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.net.HandlerAdapter;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.concurrent.Task;
import cloud.orbit.lifecycle.Startable;
import cloud.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Handles matching requests with responses
 */
public class Messaging extends HandlerAdapter implements Startable
{
    private static Object NIL = null;
    private Logger logger = LoggerFactory.getLogger(Messaging.class);

    private final AtomicInteger messageIdGen = new AtomicInteger();
    private final Map<Integer, PendingResponse> pendingResponseMap = new ConcurrentHashMap<>();

    private long responseTimeoutMillis = 30_000;

    private final LongAdder networkMessagesReceived = new LongAdder();
    private final LongAdder responsesReceived = new LongAdder();
    private static Timer timer = new Timer("OrbitMessagingTimer", true);
    private BasicRuntime runtime;


    /**
     * The case of the messageId cycling back was considered during design. It should not be a problem
     * as long as it doesn't happen in less time than the message timeout.
     *
     * Let's assume a very high number of messages per node: 1.000.000 msg/s => messageId cycles in ~4200 seconds (~70 minutes).
     * (if the message timeout remains in the order of 30s to a few minutes, that should not be a problem).
     */
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

        @Override
        public String toString()
        {
            int count;
            final StringBuilder builder = new StringBuilder()
                    .append("PendingResponse:").append(hashCode())
                    .append("[messageId=").append(messageId);
            if (isDone())
            {
                return builder.append((isCompletedExceptionally()
                        ? ",Completed exceptionally]" : ",Completed normally]")).toString();
            }
            if ((count = getNumberOfDependents()) != 0)
            {
                return builder.append(",Not completed,").append(count).append(" dependents]").toString();
            }
            return builder.append(",Not completed]").toString();
        }

    }

    public void setResponseTimeoutMillis(final long responseTimeoutMillis)
    {
        this.responseTimeoutMillis = responseTimeoutMillis;
    }

    @Override
    public Task connect(final HandlerContext ctx, final Object param) throws Exception
    {
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                cleanup();
            }
        }, 1000, 1000);

        return super.connect(ctx, param);
    }

    @Override
    public void onRead(HandlerContext ctx, Object message)
    {
        if (message instanceof Message)
        {
            this.onMessageReceived(ctx, (Message) message);
        }
        else
        {
            ctx.fireRead(message);
        }
    }

    protected void onMessageReceived(HandlerContext ctx, Message message)
    {
        // deserialize and send to runtime
        try
        {
            networkMessagesReceived.increment();
            final int messageType = message.getMessageType();
            final int messageId = message.getMessageId();
            if (logger.isTraceEnabled())
            {
                logger.trace("message received: " + message.getMessageId() + " " + message.getMessageType() + "\r\n");
            }
            final NodeAddress fromNode = message.getFromNode();
            switch (messageType)
            {
                case MessageDefinitions.REQUEST_MESSAGE:
                case MessageDefinitions.ONE_WAY_MESSAGE:
                    // forwards the message to the next inbound handler

                    int classId = message.getInterfaceId();
                    final Class classById = DefaultClassDictionary.get().getClassById(classId);
                    final RemoteReference reference = (RemoteReference) DefaultDescriptorFactory.get().getReference(
                            runtime,
                            message.getReferenceAddress(),
                            classById, message.getObjectId());

                    // todo: defer the payload decoding, in the object thread, preferably
                    int methodId = message.getMethodId();
                    final Invocation invocation = new Invocation(reference, null,
                            messageType == MessageDefinitions.ONE_WAY_MESSAGE,
                            methodId, (Object[]) message.getPayload(), null);
                    invocation.setHeaders(message.getHeaders());
                    invocation.setFromNode(message.getFromNode());
                    invocation.setMessageId(messageId);

                    if (!invocation.isOneWay())
                    {
                        Task<Object> completion = new Task<>();
                        completion.whenComplete((r, e) -> sendResponseAndLogError(ctx, fromNode, messageId,classId, methodId, r, e));
                        invocation.setCompletion(completion);
                    }
                    ctx.fireRead(invocation);
                    return;

                case MessageDefinitions.RESPONSE_OK:
                case MessageDefinitions.RESPONSE_ERROR:
                case MessageDefinitions.RESPONSE_PROTOCOL_ERROR:
                {
                    responsesReceived.increment();
                    PendingResponse pendingResponse = pendingResponseMap.remove(messageId);
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("response received: " + message.getMessageId() + " " + message.getPayload() + "\r\n" + pendingResponse);
                    }
                    if (pendingResponse != null)
                    {
                        Object res;
                        try
                        {
                            res = message.getPayload();
                        }
                        catch (Exception ex)
                        {
                            logger.error("Error deserializing response", ex);
                            pendingResponse.internalCompleteExceptionally(new UncheckedException("Error deserializing response", ex));
                            return;
                        }
                        switch (messageType)
                        {
                            case MessageDefinitions.RESPONSE_OK:
                                pendingResponse.internalComplete(res);
                                return;
                            case MessageDefinitions.RESPONSE_ERROR:
                                if (res instanceof Throwable)
                                {
                                    pendingResponse.internalCompleteExceptionally((Throwable) res);
                                }
                                else
                                {
                                    pendingResponse.internalCompleteExceptionally(new UncheckedException(String.valueOf(res)));
                                }
                                return;
                            case MessageDefinitions.RESPONSE_PROTOCOL_ERROR:
                                pendingResponse.internalCompleteExceptionally(
                                        new UncheckedException("Error invoking but no exception provided. Response: " + res));
                                return;
                            default:
                                // should be impossible
                                logger.error("Illegal protocol, invalid response message type: {}",
                                        messageType);
                                return;
                        }
                    }
                    else
                    {
                        // missing counterpart
                        logger.warn("Received response for pending request which timed out (took > " + responseTimeoutMillis + "ms) message id: {}, type: {} for {}.", messageId, messageType, getInvokedClassAndMethodName(message));
                        if (logger.isDebugEnabled()) {
                            logger.debug("Headers for message #" + messageId + " " + message.getHeaders());
                        }
                        if (logger.isTraceEnabled()) {
                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                 ObjectOutput out = new ObjectOutputStream(bos))
                            {
                                out.writeObject(message.getPayload());
                                byte[] raw = bos.toByteArray();
                                logger.trace("Payload for message #" + messageId + InternalUtils.hexDump(32, raw, 0, raw.length));
                            }
                        }
                    }
                    break;
                }
                default:
                    logger.error("Illegal protocol, invalid message type: {}", messageType);
                    return;
            }
        }
        catch (Exception ex)
        {
            logger.error("Error processing message. ", ex);
        }
    }

    private String getInvokedClassAndMethodName(Message message) {
        if (message.getInterfaceId() != 0)
        {
            final Class clazz = DefaultClassDictionary.get().getClassById(message.getInterfaceId());
            if (clazz != null)
            {
                final Method method = DefaultDescriptorFactory.get().getInvoker(clazz).getMethod(message.getMethodId());
                return clazz.getSimpleName() + "." + method.getName();
            }
        }
        return null;
    }

    protected void sendResponseAndLogError(HandlerContext ctx, final NodeAddress from, int messageId, final int classId, final int methodId, Object result, Throwable exception)
    {
        if (exception == null)
        {
            sendResponse(ctx, from, MessageDefinitions.RESPONSE_OK, messageId, classId, methodId, result);
        }
        else
        {
            sendResponse(ctx, from, MessageDefinitions.RESPONSE_ERROR, messageId, classId, methodId, exception);
        }
    }

    private Task sendResponse(HandlerContext ctx, NodeAddress to, int messageType, int messageId, final int classId, final int methodId, Object res)
    {
        return ctx.write(new Message()
                .withToNode(to)
                .withMessageId(messageId)
                .withInterfaceId(classId)
                .withMethodId(methodId)
                .withMessageType(messageType)
                .withPayload(res));
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            return writeInvocation(ctx, (Invocation) msg);
        }
        if (msg instanceof Message)
        {
            return writeMessage(ctx, (Message) msg);
        }
        return super.write(ctx, msg);
    }

    public Task<?> writeInvocation(final HandlerContext ctx, Invocation invocation)
    {
        final Addressable toReference = invocation.getToReference();
        RemoteReference<?> actorReference = (RemoteReference<?>) toReference;
        NodeAddress toNode = invocation.getToNode();

        Map<Object, Object> actualHeaders = null;
        if (invocation.getHeaders() != null)
        {
            actualHeaders = new HashMap<>();
            actualHeaders.putAll(invocation.getHeaders());
        }

        final Message message = new Message()
                .withMessageType(invocation.isOneWay() ? MessageDefinitions.ONE_WAY_MESSAGE : MessageDefinitions.REQUEST_MESSAGE)
                .withToNode(toNode)
                .withFromNode(invocation.getFromNode())
                .withHeaders(actualHeaders)
                .withInterfaceId(actorReference._interfaceId())
                .withMessageId(invocation.getMessageId())
                .withMethodId(invocation.getMethodId())
                .withObjectId(RemoteReference.getId(actorReference))
                .withPayload(invocation.getParams())
                .withReferenceAddress(invocation.getToReference().address);


        if (logger.isTraceEnabled())
        {
            logger.trace("sending message to " + toReference);
        }

        return writeMessage(ctx, message);
    }

    public Task<?> writeMessage(HandlerContext ctx, Message message)
    {
        int messageId = message.getMessageId();
        if (messageId != 0)
        {
            // forwarding message somewhere, hosting's doing, likely
            // occurs when the object ownership changes
            if (logger.isDebugEnabled())
            {
                logger.debug("Forwarding message: " + messageId + " to " + message.getToNode().asUUID().getLeastSignificantBits());
            }
            return ctx.write(message);
        }

        messageId = messageIdGen.incrementAndGet();
        message.setMessageId(messageId);
        if (logger.isTraceEnabled())
        {
            logger.trace("sending message " + messageId);
        }

        if (message.getMessageType() != MessageDefinitions.REQUEST_MESSAGE)
        {
            ctx.write(message);
            return Task.done();
        }
        message.setMessageId(messageId);
        PendingResponse pendingResponse = new PendingResponse(messageId, runtime.clock().millis() + responseTimeoutMillis);
        final boolean oneWay = message.getMessageType() == MessageDefinitions.ONE_WAY_MESSAGE;
        if (!oneWay)
        {
            pendingResponseMap.put(messageId, pendingResponse);
        }
        try
        {
            ctx.write(message);
            if (oneWay)
            {
                pendingResponse.internalComplete(NIL);
            }
        }
        catch (Exception ex)
        {
            pendingResponseMap.remove(messageId);
            pendingResponse.internalCompleteExceptionally(ex);
        }
        return pendingResponse;
    }

    public Task cleanup()
    {
        // The benchmarks showed that it's faster to iterate over all pending messages
        // than to keep a priority queue ordered by timeout.

        // The priority queue forces the application to pay the price of keeping the queue ordered
        // plus the synchronization cost of adding and removing elements.

        pendingResponseMap.values()
                .forEach(top -> {
                    if (top.timeoutAt > runtime.clock().millis())
                    {
                        // return the message, if there was a concurrent reception the message will be removed on the next cycle.
                        return;
                    }
                    if (!top.isDone())
                    {
                        // todo: do this in the application executor, not critical as hosting is already taking care of it
                        top.internalCompleteExceptionally(new TimeoutException("Response timeout"));
                    }
                    pendingResponseMap.remove(top.messageId);
                });
        return Task.done();
    }

    public BasicRuntime getRuntime()
    {
        return runtime;
    }

    public void setRuntime(final BasicRuntime runtime)
    {
        this.runtime = runtime;
        logger = runtime.getLogger(this);
    }

}
