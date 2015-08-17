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

import com.ea.orbit.actors.cluster.ClusterPeer;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class Messaging implements Startable
{
    private static Object NIL = null;
    private static final Logger logger = LoggerFactory.getLogger(Messaging.class);
    // consults with Hosting to determine the target server to send the message to.
    // serializes the messages
    // pass received messages to Execution

    private ClusterPeer clusterPeer;
    private Execution execution;
    private final AtomicInteger messageIdGen = new AtomicInteger();
    private final Map<Integer, PendingResponse> pendingResponseMap = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<PendingResponse> pendingResponsesQueue = new PriorityBlockingQueue<>(50, new PendingResponseComparator());
    private Clock clock = Clock.systemUTC();

    @Config("orbit.actors.defaultMessageTimeout")
    private long responseTimeoutMillis = 30_000;

    private final LongAdder networkMessagesReceived = new LongAdder();
    private final LongAdder objectMessagesReceived = new LongAdder();
    private final LongAdder responsesReceived = new LongAdder();
    private ExecutorService executor;
    protected MessageSerializer messageSerializer = new JavaMessageSerializer();

    public void setExecution(final Execution execution)
    {
        this.execution = execution;
    }

    public void setClusterPeer(final ClusterPeer clusterPeer)
    {
        this.clusterPeer = clusterPeer;
    }

    public void setResponseTimeoutMillis(final long responseTimeoutMillis)
    {
        this.responseTimeoutMillis = responseTimeoutMillis;
    }

    public NodeAddress getNodeAddress()
    {
        return clusterPeer.localAddress();
    }

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

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public Task<?> start()
    {
        if (executor == null)
        {
            executor = ExecutorUtils.newScalingThreadPool(64);
        }
        clusterPeer.registerMessageReceiver((from, buff) -> executor.execute(() -> onMessageReceived(from, buff)));
        //timeoutCleanup()
        return Task.done();
    }

    @Override
    public Task<?> stop()
    {
        executor.shutdown();
        try
        {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        return Task.done();
    }

    private void onMessageReceived(final NodeAddress from, final byte[] buff)
    {
        // deserialize and send to runtime
        try
        {
            networkMessagesReceived.increment();
            Message message = messageSerializer.deserializeMessage(execution, new ByteArrayInputStream(buff));
            message.withFromNode(from);
            switch (message.getMessageType())
            {
                case MessageDefinitions.NORMAL_MESSAGE:
                case MessageDefinitions.ONEWAY_MESSAGE:
                    execution.onMessageReceived(message);
                    return;

                case MessageDefinitions.NORMAL_RESPONSE:
                case MessageDefinitions.EXCEPTION_RESPONSE:
                case MessageDefinitions.ERROR_RESPONSE:
                {
                    responsesReceived.increment();
                    PendingResponse pendingResponse = pendingResponseMap.remove(message.getMessageId());
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
                        switch (message.getMessageType())
                        {
                            case MessageDefinitions.NORMAL_RESPONSE:
                                pendingResponse.internalComplete(res);
                                return;
                            case MessageDefinitions.EXCEPTION_RESPONSE:
                                pendingResponse.internalCompleteExceptionally((Throwable) res);
                                return;
                            case MessageDefinitions.ERROR_RESPONSE:
                                pendingResponse.internalCompleteExceptionally(
                                        new UncheckedException("Error invoking but no exception provided. Response: " + res));
                                return;
                            default:
                                // should be impossible
                                logger.error("Illegal protocol, invalid response message type: {}",
                                        message.getMessageType());
                                return;
                        }
                    }
                    else
                    {
                        // missing counterpart
                        logger.warn("Missing counterpart (pending message) for message with id: {} and type: {}.",
                                message.getMessageId(), message.getMessageType());
                    }
                    break;
                }
                default:
                    logger.error("Illegal protocol, invalid message type: {}", message.getMessageType());
                    return;
            }
        }
        catch (Exception ex)
        {
            logger.error("Error processing message. ", ex);
        }
    }

    public void onNodeDrop(final NodeAddress address)
    {
        // could be used to decrease the timeout of messages sent to failed nodes.
    }

    public void sendResponse(NodeAddress to, int messageType, int messageId, Object res)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try
        {
            messageSerializer.serializeMessage(execution, byteArrayOutputStream,
                    new Message()
                            .withMessageId(messageId)
                            .withMessageType(messageType)
                            .withPayload(res));
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
        clusterPeer.sendMessage(to, byteArrayOutputStream.toByteArray());
    }

    public Task<?> sendMessage(Message message)
    {
        int messageId = messageIdGen.incrementAndGet();
        message.setMessageId(messageId);
        PendingResponse pendingResponse = new PendingResponse(messageId, clock.millis() + responseTimeoutMillis);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try
        {
            messageSerializer.serializeMessage(execution, byteArrayOutputStream, message);
        }
        catch (Exception | Error e)
        {
            if (logger.isErrorEnabled())
            {
                logger.error("Error sending message", e);
            }
            return Task.fromException(new UncheckedException(e));
        }
        final boolean oneWay = message.isOneWay();
        if (!oneWay)
        {

            pendingResponseMap.put(messageId, pendingResponse);
            pendingResponsesQueue.add(pendingResponse);
        }
        try
        {
            clusterPeer.sendMessage(message.getToNode(), byteArrayOutputStream.toByteArray());
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

    public void setExecutor(final ExecutorService pool)
    {
        this.executor = pool;
    }

}
