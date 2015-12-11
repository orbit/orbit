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

import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskContext;
import com.ea.orbit.container.Startable;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
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
    private final PriorityBlockingQueue<PendingResponse> pendingResponsesQueue = new PriorityBlockingQueue<>(50, new PendingResponseComparator());

    @Config("orbit.actors.defaultMessageTimeout")
    private long responseTimeoutMillis = 30_000;

    private final LongAdder networkMessagesReceived = new LongAdder();
    private final LongAdder responsesReceived = new LongAdder();
    private static Timer timer = new Timer("Messaging timer");
    private BasicRuntime runtime;

    /**
     * The messageId is used only to break a tie between messages that timeout at the same ms.
     */
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
            final NodeAddress fromNode = message.getFromNode();
            switch (messageType)
            {
                case MessageDefinitions.REQUEST_MESSAGE:
                case MessageDefinitions.ONE_WAY_MESSAGE:
                    // forwards the message to the next inbound handler

                    final Class classById = DefaultClassDictionary.get().getClassById(message.getInterfaceId());
                    final RemoteReference reference = (RemoteReference) DefaultDescriptorFactory.get().getReference(
                            runtime,
                            message.getReferenceAddress(),
                            classById, message.getObjectId());

                    // todo: defer the payload decoding, in the object thread, preferably
                    final Invocation invocation = new Invocation(reference, null,
                            messageType == MessageDefinitions.ONE_WAY_MESSAGE,
                            message.getMethodId(), (Object[]) message.getPayload(), null);
                    invocation.setHeaders(message.getHeaders());
                    invocation.setFromNode(message.getFromNode());
                    invocation.setMessageId(messageId);

                    if (!invocation.isOneWay())
                    {
                        Task<Object> completion = new Task<>();
                        completion.whenComplete((r, e) -> sendResponseAndLogError(ctx, fromNode, messageId, r, e));
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
                        pendingResponsesQueue.remove(pendingResponse);
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
                        logger.warn("Missing counterpart (pending message) for message with id: {} and type: {}.",
                                messageId, messageType);
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
            logger.error("Error processing   message. ", ex);
        }
    }

    protected void sendResponseAndLogError(HandlerContext ctx, final NodeAddress from, int messageId, Object result, Throwable exception)
    {
        if (exception == null)
        {
            sendResponse(ctx, from, MessageDefinitions.RESPONSE_OK, messageId, result);
        }
        else
        {
            sendResponse(ctx, from, MessageDefinitions.RESPONSE_ERROR, messageId, exception);
        }
    }

    private Task sendResponse(HandlerContext ctx, NodeAddress to, int messageType, int messageId, Object res)
    {
        return ctx.write(new Message()
                .withToNode(to)
                .withMessageId(messageId)
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

        final LinkedHashMap<Object, Object> actualHeaders = new LinkedHashMap<>();
        if (invocation.getHeaders() != null)
        {
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
            pendingResponsesQueue.add(pendingResponse);
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
            pendingResponsesQueue.remove(pendingResponse);
            pendingResponse.internalCompleteExceptionally(ex);
        }
        return pendingResponse;
    }

    public Task cleanup()
    {
        PendingResponse top = pendingResponsesQueue.peek();
        if (top != null && top.timeoutAt < runtime.clock().millis())
        {
            for (; (top = pendingResponsesQueue.poll()) != null; )
            {
                if (top.timeoutAt > runtime.clock().millis())
                {
                    // return the message, if there was a concurrent reception the message will be removed on the next cycle.
                    pendingResponsesQueue.add(top);
                    break;
                }
                if (!top.isDone())
                {
                    top.internalCompleteExceptionally(new TimeoutException("Response timeout"));
                }
                pendingResponseMap.remove(top.messageId);
            }
        }
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
