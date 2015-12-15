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

import com.ea.orbit.actors.extensions.NamedPipelineExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.cloner.ExecutionObjectCloner;
import com.ea.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Local messages traverse the protocol stack just beyond Messaging and then are bounced back.
 * Their payload is cloned, this avoids full payload serialization.
 */
public class MessageLoopback extends NamedPipelineExtension
{
    private static Logger logger = LoggerFactory.getLogger(MessageLoopback.class);
    private ExecutionObjectCloner cloner;
    private ActorRuntime runtime;

    public MessageLoopback()
    {
        super(DefaultHandlers.MESSAGE_LOOPBACK, null, DefaultHandlers.MESSAGING);
    }

    private static final Set<Class> immutables = new HashSet<>(Arrays.asList(
            java.lang.String.class,
            java.lang.Integer.class,
            java.lang.Byte.class,
            java.lang.Character.class,
            java.lang.Short.class,
            java.lang.Boolean.class,
            java.lang.Long.class,
            java.lang.Double.class,
            java.lang.Float.class,
            java.lang.StackTraceElement.class,
            java.math.BigInteger.class,
            java.math.BigDecimal.class,
            java.io.File.class,
            java.util.Locale.class,
            java.util.UUID.class,
            java.util.Collections.class,
            java.net.URL.class,
            java.net.URI.class,
            java.net.Inet4Address.class,
            java.net.Inet6Address.class,
            java.net.InetSocketAddress.class
    ));

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Message)
        {
            Message message = (Message) msg;
            if (Objects.equals(message.getToNode(), runtime.getLocalAddress()))
            {
                if (needsCloning(message))
                {
                    if (cloner == null)
                    {
                        return ctx.write(msg);
                    }
                    final Object originalPayload = message.getPayload();
                    final Object clonedPayload;
                    runtime.bind();
                    try
                    {
                        clonedPayload = cloner.clone(originalPayload);
                    }
                    catch (Exception e)
                    {
                        // ignoring clone errors
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Error cloning message: " + message, e);
                        }
                        return ctx.write(msg);
                    }
                    message.setPayload(clonedPayload);
                }
                // short circuits the message back
                ctx.fireRead(message);
                return Task.done();
            }
        }
        return ctx.write(msg);
    }

    protected boolean needsCloning(final Message message)
    {
        // this can be improved by looking into the
        // argument/return types of ClassId.MethodId and caching the decision.

        final Object payload = message.getPayload();
        if (payload == null)
        {
            return false;
        }
        switch (message.getMessageType())
        {
            case MessageDefinitions.ONE_WAY_MESSAGE:
            case MessageDefinitions.REQUEST_MESSAGE:
                if (payload instanceof Object[])
                {
                    Object[] arr = (Object[]) payload;
                    for (int i = arr.length; --i >= 0; )
                    {
                        Object obj = arr[i];
                        if (obj != null && !immutables.contains(obj.getClass()))
                        {
                            return true;
                        }
                    }
                    // no cloning required, true also fir arr.length = 0
                    return false;
                }
                break;
            case MessageDefinitions.RESPONSE_OK:
                // already test for null in the beginning of the method.
                if (immutables.contains(payload.getClass()))
                {
                    return false;
                }
                break;
        }
        return true;
    }

    public ExecutionObjectCloner getCloner()
    {
        return cloner;
    }

    public void setCloner(final ExecutionObjectCloner cloner)
    {
        this.cloner = cloner;
    }

    public ActorRuntime getRuntime()
    {
        return runtime;
    }

    public void setRuntime(final ActorRuntime runtime)
    {
        this.runtime = runtime;
    }
}