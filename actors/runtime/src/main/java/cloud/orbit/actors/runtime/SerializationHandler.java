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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.net.HandlerAdapter;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import cloud.orbit.tuples.Pair;

import java.io.ByteArrayInputStream;

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
        if (!(msg instanceof Message))
        {
            return ctx.write(msg);
        }
        final Message message = (Message) msg;

        try
        {
            return ctx.write(Pair.of(message.getToNode(), messageSerializer.serializeMessage(runtime, message)));
        }
        catch (Exception ex2)
        {
            final int messageType = message.getMessageType();
            if (messageType == MessageDefinitions.REQUEST_MESSAGE
                    || messageType == MessageDefinitions.ONE_WAY_MESSAGE)
            {
                logger.error("Error serializing message", ex2);
                //return Task.fromException(ex2);
                throw new UncheckedException("Error serializing message", ex2);
            }
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
                return ctx.write(Pair.of(message.getToNode(), messageSerializer.serializeMessage(runtime, message)));
            }
            catch (Exception ex3)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed twice sending result. ", ex2);
                }
                try
                {
                    message.withMessageType(MessageDefinitions.RESPONSE_PROTOCOL_ERROR)
                            .withPayload("failed twice sending response");
                    return ctx.write(Pair.of(message.getToNode(), messageSerializer.serializeMessage(runtime, message)));
                }
                catch (Exception ex4)
                {
                    throw new UncheckedException("Failed sending exception. ", ex4);
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

    @Override
    public void onRead(HandlerContext ctx, final Object msg)
    {
        Pair<NodeAddress, byte[]> message = (Pair<NodeAddress, byte[]>) msg;
        final ByteArrayInputStream bais = new ByteArrayInputStream(message.getRight());
        Message msg1 = null;
        try
        {
            msg1 = messageSerializer.deserializeMessage(runtime, bais);
            if (msg1.getFromNode() == null)
            {
                msg1.setFromNode(message.getLeft());
            }
        }
        catch (Throwable e)
        {
            logger.error("Error deserializing message", e);
            logger.error(InternalUtils.hexDump(32, message.getRight(), 0, message.getRight().length));
        }
        if (msg1 != null)
        {
            ctx.fireRead(msg1);
        }
    }


}
