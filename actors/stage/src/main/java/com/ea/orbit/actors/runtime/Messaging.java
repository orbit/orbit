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

import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.net.Channel;
import com.ea.orbit.actors.net.ChannelHandler;
import com.ea.orbit.actors.net.ChannelHandlerAdapter;
import com.ea.orbit.actors.net.ChannelHandlerContext;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

public class Messaging extends ChannelHandlerAdapter implements Startable
{
    private static Object NIL = null;
    private static final Logger logger = LoggerFactory.getLogger(Messaging.class);
    // serializes the messages
    // pass received messages to Execution

    private final AtomicInteger messageIdGen = new AtomicInteger();
    private final Map<Integer, PendingResponse> pendingResponseMap = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<PendingResponse> pendingResponsesQueue = new PriorityBlockingQueue<>(50, new PendingResponseComparator());
    private Clock clock = Clock.systemUTC();

    @Config("orbit.actors.defaultMessageTimeout")
    private long responseTimeoutMillis = 30_000;

    private final LongAdder networkMessagesReceived = new LongAdder();
    private final LongAdder responsesReceived = new LongAdder();
    private Function<Message, Task<?>> invocationHandler;

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
    }

    public void setResponseTimeoutMillis(final long responseTimeoutMillis)
    {
        this.responseTimeoutMillis = responseTimeoutMillis;
    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    @Override
    public void onRead(ChannelHandlerContext ctx, Object message)
    {
        this.onMessageReceived(ctx, (Message) message);
    }

    protected void onMessageReceived(ChannelHandlerContext ctx, Message message)
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
                    handleInvocation(message)
                            .handle((r, e) -> {
                                        sendResponseAndLogError(ctx,
                                                messageType == MessageDefinitions.ONE_WAY_MESSAGE,
                                                fromNode, messageId, r, e);
                                        return null;
                                    }
                            );
                    return;

                case MessageDefinitions.RESPONSE_OK:
                case MessageDefinitions.RESPONSE_ERROR:
                case MessageDefinitions.RESPONSE_PROTOCOL_ERROR:
                {
                    responsesReceived.increment();
                    PendingResponse pendingResponse = pendingResponseMap.remove(messageId);
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
                                pendingResponse.internalCompleteExceptionally((Throwable) res);
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

    protected Task<?> handleInvocation(final Message message)
    {
        if (invocationHandler == null)
        {
            return Task.fromException(new UncheckedException("No invocationHandler defined"));
        }
        try
        {
            return invocationHandler.apply(message);
        }
        catch (Throwable ex)
        {
            return Task.fromException(ex);
        }
    }

    public void setInvocationHandler(Function<Message, Task<?>> listener)
    {
        invocationHandler = listener;
    }


    private void sendResponse(ChannelHandlerContext ctx, NodeAddress to, int messageType, int messageId, Object res)
    {
        ctx.write(new Message()
                .withToNode(to)
                .withMessageId(messageId)
                .withMessageType(messageType)
                .withPayload(res));
    }

    public Task<?> sendMessage(ChannelHandlerContext ctx, Message message)
    {
        int messageId = messageIdGen.incrementAndGet();
        message.setMessageId(messageId);
        PendingResponse pendingResponse = new PendingResponse(messageId, clock.millis() + responseTimeoutMillis);
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

    public void timeoutCleanup()
    {
        PendingResponse top = pendingResponsesQueue.peek();
        if (top != null && top.timeoutAt < clock.millis())
        {
            for (; (top = pendingResponsesQueue.poll()) != null; )
            {
                if (top.timeoutAt > clock.millis())
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
    }

    protected void sendResponseAndLogError(ChannelHandlerContext ctx, boolean oneway, final NodeAddress from, int messageId, Object result, Throwable exception)
    {
        if (exception != null && logger.isDebugEnabled())
        {
            logger.debug("Unknown application error. ", exception);
        }

        if (!oneway)
        {
            try
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
            catch (Exception ex2)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Error sending method result", ex2);
                }
                try
                {
                    if (exception != null)
                    {
                        sendResponse(ctx, from, MessageDefinitions.RESPONSE_ERROR, messageId, toSerializationSafeException(exception, ex2));
                    }
                    else
                    {
                        sendResponse(ctx, from, MessageDefinitions.RESPONSE_ERROR, messageId, ex2);
                    }
                }
                catch (Exception ex3)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Failed twice sending result. ", ex2);
                    }
                    try
                    {
                        sendResponse(ctx, from, MessageDefinitions.RESPONSE_PROTOCOL_ERROR, messageId, "failed twice sending result");
                    }
                    catch (Exception ex4)
                    {
                        logger.error("Failed sending exception. ", ex4);
                    }
                }
            }
        }
    }

    private Throwable toSerializationSafeException(final Throwable notSerializable, final Throwable secondaryException)
    {
        final UncheckedException runtimeException = new UncheckedException(secondaryException.getMessage(), secondaryException, true, true);
        for (Throwable t = notSerializable; t != null; t = t.getCause())
        {
            final RuntimeException newEx = new RuntimeException(
                    t.getMessage() == null
                            ? t.getClass().getName()
                            : (t.getClass().getName() + ": " + t.getMessage()));
            newEx.setStackTrace(t.getStackTrace());
            runtimeException.addSuppressed(newEx);
        }
        return runtimeException;
    }

}
