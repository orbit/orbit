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

import cloud.orbit.actors.extensions.NamedPipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.cloner.CloneHelper;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

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
                    catch (LinkageError | Exception e)
                    {
                        // Log clone errors as warnings
                        if (logger.isWarnEnabled())
                        {
                            logger.warn("Unable to clone message: " + message, e);
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
        return CloneHelper.needsCloning(message);
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