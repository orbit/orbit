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

package com.ea.orbit.actors.ws.test;

import com.ea.orbit.actors.extensions.json.JsonMessageSerializer;
import com.ea.orbit.actors.net.ChannelHandler;
import com.ea.orbit.actors.net.ChannelHandlerContext;
import com.ea.orbit.actors.runtime.ActorRuntime;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.concurrent.Task;

import java.io.ByteArrayInputStream;

public class SerializerHandler implements ChannelHandler
{
    JsonMessageSerializer json = new JsonMessageSerializer();
    ProtoMessageSerializer proto = new ProtoMessageSerializer();

    @Override
    public void onRead(final ChannelHandlerContext ctx, final Object message)
    {
        final byte[] bytes = (byte[]) message;
        final Message newMessage;
        if (bytes[0] == '{')
        {
            try
            {
                newMessage = json.deserializeMessage(ActorRuntime.getRuntime(), new ByteArrayInputStream(bytes));
                ctx.fireChannelRead(newMessage);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return;
        }
        try
        {
            newMessage = proto.deserializeMessage(ActorRuntime.getRuntime(), new ByteArrayInputStream(bytes));
            ctx.fireChannelRead(newMessage);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return;

    }

    @Override
    public void onStart(final ChannelHandlerContext ctx)
    {
        ctx.fireChannelActive();
    }

    @Override
    public Task write(final ChannelHandlerContext ctx, final Object message)
    {
        return null;
    }
}
