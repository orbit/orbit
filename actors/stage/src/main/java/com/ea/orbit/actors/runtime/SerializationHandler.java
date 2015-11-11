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
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.tuples.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SerializationHandler extends HandlerAdapter
{
    private static Logger logger = LoggerFactory.getLogger(SerializationHandler.class);
    private BasicRuntime runtime;
    private MessageSerializer messageSerializer;

    public SerializationHandler(final BasicRuntime runtime, final MessageSerializer messageSerializer)
    {
        this.runtime = runtime;
        this.messageSerializer = messageSerializer;
    }

    @Override
    public Task write(HandlerContext ctx, final Object msg)
    {
        Message message = (Message) msg;
        if (message.getToNode() == null)
        {
            throw new UncheckedException("Message.toNode must be defined");
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            messageSerializer.serializeMessage(runtime, baos, message);
        }
        catch (Exception ex2)
        {
            final int messageType = message.getMessageType();
            if (messageType != MessageDefinitions.RESPONSE_OK
                    && messageType != MessageDefinitions.RESPONSE_ERROR)
            {
                return Task.fromException(ex2);
            }
            baos.reset();
            if (logger.isDebugEnabled())
            {
                logger.debug("Error sending response", ex2);
            }
            try
            {
                final Object payload = message.getPayload();
                if (messageType == MessageDefinitions.RESPONSE_ERROR && payload instanceof Throwable)
                {
                    message.withMessageType(MessageDefinitions.RESPONSE_ERROR)
                            .withPayload(toSerializationSafeException((Throwable) payload, ex2));
                }
                else
                {
                    message.withMessageType(MessageDefinitions.RESPONSE_ERROR)
                            .withPayload(ex2);
                }
                messageSerializer.serializeMessage(runtime, baos, message);
            }
            catch (Exception ex3)
            {
                baos.reset();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed twice sending result. ", ex2);
                }
                try
                {
                    message.withMessageType(MessageDefinitions.RESPONSE_ERROR)
                            .withPayload("failed twice sending response");
                    messageSerializer.serializeMessage(runtime, baos, message);
                }
                catch (Exception ex4)
                {
                    logger.error("Failed sending exception. ", ex4);
                }
            }
        }

        return ctx.write(Pair.of(message.getToNode(), baos.toByteArray()));
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

    @Override
    public void onRead(HandlerContext ctx, final Object msg)
    {
        Pair<NodeAddress, byte[]> message = (Pair<NodeAddress, byte[]>) msg;
        final ByteArrayInputStream bais = new ByteArrayInputStream(message.getRight());
        try
        {
            ctx.fireRead(messageSerializer
                    .deserializeMessage(runtime, bais)
                    .withFromNode(message.getLeft()));
        }
        catch (Exception e)
        {
            logger.error("Error deserializing message", e);
        }
    }
}
