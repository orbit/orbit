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

package com.ea.orbit.actors.server;

import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;
import com.ea.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

class ServerPeerExecutor extends HandlerAdapter
{
    private static Logger logger = LoggerFactory.getLogger(ServerPeerExecutor.class);
    private final Stage stage;

    public ServerPeerExecutor(final Stage stage)
    {
        this.stage = stage;
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        // TODO: server-to-client invocation ?
        return super.write(ctx, msg);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRead(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Message)
        {
            final Message message = (Message) msg;
            final int messageType = message.getMessageType();
            final NodeAddress fromNode = message.getFromNode();
            final int messageId = message.getMessageId();
            switch (messageType)
            {
                case MessageDefinitions.ONE_WAY_MESSAGE:
                case MessageDefinitions.REQUEST_MESSAGE:
                    final Class<?> clazz = DefaultClassDictionary.get().getClassById(message.getInterfaceId());
                    final Addressable reference = (Addressable)stage.getReference((Class)clazz, message.getObjectId());
                    final boolean oneWay = message.getMessageType() == MessageDefinitions.ONE_WAY_MESSAGE;
                    final Method method = stage.getInvoker(message.getInterfaceId()).getMethod(message.getMethodId());
                    final Task<?> res = stage.invoke(reference, method,
                            oneWay, message.getMethodId(),
                            (Object[]) message.getPayload());
                    res.whenComplete((r, e) ->
                            sendResponseAndLogError(ctx,
                                    messageType == MessageDefinitions.ONE_WAY_MESSAGE,
                                    fromNode, messageId, r, e));
                    break;
            }
        }
    }


    protected void sendResponseAndLogError(HandlerContext ctx, boolean oneway, final NodeAddress from, int messageId, Object result, Throwable exception)
    {
        if (exception != null && logger.isDebugEnabled())
        {
            logger.debug("Unknown application error. ", exception);
        }

        if (!oneway)
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
    }

    private Task sendResponse(HandlerContext ctx, NodeAddress to, int messageType, int messageId, Object res)
    {
        return ctx.write(new Message()
                .withToNode(to)
                .withMessageId(messageId)
                .withMessageType(messageType)
                .withPayload(res));
    }
}
