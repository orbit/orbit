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

import com.ea.orbit.actors.IActorObserver;
import com.ea.orbit.actors.cluster.IClusterPeer;
import com.ea.orbit.actors.cluster.INodeAddress;
import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Messaging implements Startable
{
    private static Object NIL = null;
    private static final Logger logger = LoggerFactory.getLogger(Messaging.class);
    // consults with Hosting to determine the target server to send the message to.
    // serializes the messages
    // pass received messages to Execution

    private IClusterPeer clusterPeer;
    private Execution execution;
    private AtomicInteger messageIdGen = new AtomicInteger();
    private Map<Integer, PendingResponse> pendingResponseMap = new ConcurrentHashMap<>();
    private PriorityBlockingQueue<PendingResponse> pendingResponsesQueue = new PriorityBlockingQueue<>();
    private Clock clock = Clock.systemUTC();
    private long responseTimeoutMillis = 30_000;
    private AtomicLong networkMessagesReceived = new AtomicLong();
    private AtomicLong objectMessagesReceived = new AtomicLong();
    private AtomicLong responsesReceived = new AtomicLong();
    private ExecutorService executor;

    public void setExecution(final Execution execution)
    {
        this.execution = execution;
    }

    public void setClusterPeer(final IClusterPeer clusterPeer)
    {
        this.clusterPeer = clusterPeer;
    }

    public INodeAddress getNodeAddress()
    {
        return clusterPeer.localAddress();
    }

    private static class PendingResponse extends Task implements Comparable<PendingResponse>
    {
        long timeoutAt;
        public int messageId;

        @Override
        public int compareTo(final PendingResponse o)
        {
            int cmp = Long.compare(timeoutAt, o.timeoutAt);
            if (cmp == 0)
            {
                return messageId - o.messageId;
            }
            return cmp;
        }

        @Override
        public boolean equals(final Object o)
        {
            // adding equals implementation to silence FindBugs
            return (this == o);
        }

    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public Task start()
    {
        if (executor == null)
        {
            executor = ExecutorUtils.newScalingThreadPool(1000);
        }
        clusterPeer.registerMessageReceiver((from, buff) -> executor.execute(() -> onMessageReceived(from, buff)));
        //timeoutCleanup()
        return Task.done();
    }

    @Override
    public Task stop()
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

    private void onMessageReceived(final INodeAddress from, final byte[] buff)
    {
        // deserialize and send to runtime
        try
        {
            networkMessagesReceived.incrementAndGet();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buff));
            byte messageType = in.readByte();
            int messageId = in.readInt();
            switch (messageType)
            {
                case 0:
                case 8:
                    boolean oneway = (messageType == 8);
                    objectMessagesReceived.incrementAndGet();
                    int interfaceId = in.readInt();
                    int methodId = in.readInt();
                    Object key = in.readObject();
                    Object[] params = (Object[]) in.readObject();
                    execution.onMessageReceived(from, oneway, messageId, interfaceId, methodId, key, params);
                    break;
                case 1:
                case 2:
                case 3:
                {
                    responsesReceived.incrementAndGet();
                    // 1 - normal response
                    // 2 - exception response
                    // 3 - error but exception provided
                    PendingResponse pendingResponse = pendingResponseMap.remove(messageId);
                    if (pendingResponse != null)
                    {
                        pendingResponsesQueue.remove(pendingResponse);
                        Object res;
                        try
                        {
                            res = in.readObject();
                        }
                        catch (Exception ex)
                        {
                            logger.error("Error deserializing response", ex);
                            pendingResponse.completeExceptionally(new UncheckedException("Error deserializing response", ex));
                            return;
                        }
                        switch (messageType)
                        {
                            case 1:
                                pendingResponse.complete(res);
                                return;
                            case 2:
                                pendingResponse.completeExceptionally((Throwable) res);
                                return;
                            case 3:
                                pendingResponse.completeExceptionally(new UncheckedException("Error invoking but no exception provided. Res: " + res));
                                return;
                            default:
                                // should be impossible
                                logger.error("Illegal protocol, invalid response message type: {}", messageId);
                                return;
                        }
                    }
                    else
                    {
                        // missing counterpart
                        logger.warn("Missing counterpart (pending message) for message {}.", messageId);
                    }
                    break;
                }
                default:
                    logger.error("Illegal protocol, invalid message type: {}", messageId);
                    return;
            }
        }
        catch (Exception ex)
        {
            logger.error("Error processing message. ", ex);
        }
    }

    public void onNodeDrop(final INodeAddress address)
    {
        // could be used to decrease the timeout of messages sent to failed nodes.
    }


    public void sendResponse(INodeAddress to, int messageType, int messageId, Object res)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try
        {
            ObjectOutput objectOutput = createObjectOutput(byteArrayOutputStream);
            objectOutput.writeByte(messageType);
            objectOutput.writeInt(messageId);
            objectOutput.writeObject(res);
            objectOutput.flush();
        }
        catch (IOException e)
        {
            throw new UncheckedException(e);
        }
        clusterPeer.sendMessage(to, byteArrayOutputStream.toByteArray());
    }

    private ObjectOutput createObjectOutput(final OutputStream outputStream) throws IOException
    {
        return new ObjectOutputStream(outputStream)
        {
            {
                enableReplaceObject(true);
            }

            @Override
            protected Object replaceObject(final Object obj) throws IOException
            {
                if (!(obj instanceof ActorReference))
                {
                    if (obj instanceof OrbitActor)
                    {
                        return ((OrbitActor) obj).reference;
                    }
                    if (obj instanceof IActorObserver)
                    {
                        return execution.getObjectReference(null, (IActorObserver) obj);
                    }
                }
                return super.replaceObject(obj);
            }
        };
    }

    public Task<?> sendMessage(INodeAddress to, boolean oneWay, int interfaceId, int methodId, Object key, Object[] params)
    {
        int messageId = messageIdGen.incrementAndGet();
        PendingResponse pendingResponse = new PendingResponse();
        pendingResponse.messageId = messageId;
        pendingResponse.timeoutAt = clock.millis() + responseTimeoutMillis;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try
        {
            ObjectOutput objectOutput = createObjectOutput(byteArrayOutputStream);
            objectOutput.writeByte(oneWay ? 8 : 0);
            objectOutput.writeInt(messageId);
            objectOutput.writeInt(interfaceId);
            objectOutput.writeInt(methodId);
            objectOutput.writeObject(key);
            objectOutput.writeObject(params);
            objectOutput.flush();
        }
        catch (Exception | Error e)
        {
            if (logger.isErrorEnabled())
            {
                logger.error("Error sending message to object key " + key, e);
            }
            throw new UncheckedException(e);
        }
        if (!oneWay)
        {
            pendingResponseMap.put(messageId, pendingResponse);
            pendingResponsesQueue.add(pendingResponse);
        }
        try
        {
            clusterPeer.sendMessage(to, byteArrayOutputStream.toByteArray());
            if (oneWay)
            {
                pendingResponse.complete(NIL);
            }
        }
        catch (Exception ex)
        {
            pendingResponseMap.remove(messageId);
            pendingResponsesQueue.remove(pendingResponse);
            pendingResponse.completeExceptionally(ex);
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
                    top.completeExceptionally(new TimeoutException("Response timeout"));
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
