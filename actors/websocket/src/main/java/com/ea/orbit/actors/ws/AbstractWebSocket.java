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

package com.ea.orbit.actors.ws;

import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.extensions.json.JsonMessageSerializer;
import com.ea.orbit.actors.net.Handler;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.Peer;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.tuples.Pair;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import java.nio.ByteBuffer;

public abstract class AbstractWebSocket
{
    protected Session session;
    protected HandlerContext peerContext;
    protected Handler network = new WebSocketNetwork();

    public void setMessageSerializer(MessageSerializer serializer)
    {
        peer().setMessageSerializer(serializer);
    }

    protected abstract Peer peer();


    @OnOpen
    public void onOpen(Session wsSession)
    {
        this.session = wsSession;

        if (peer().getMessageSerializer() == null)
        {
            peer().setMessageSerializer(new JsonMessageSerializer());
        }
        peer().setNetworkHandler(network);
        peer().start().join();
        peerContext.fireActive();
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason)
    {
        peerContext.fireInactive();
    }

    @OnMessage
    public void onMessage(byte[] message, boolean last, Session session)
    {
        peerContext.fireRead(Pair.of(null, message));
    }

    public Task close()
    {
        return peer().getPipeline().close();
    }

    private class WebSocketNetwork extends HandlerAdapter
    {
        @Override
        public void onRegistered(final HandlerContext ctx) throws Exception
        {
            peerContext = ctx;
            super.onRegistered(ctx);
        }

        @Override
        public Task write(final HandlerContext ctx, final Object msg) throws Exception
        {
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap((byte[]) ((Pair) msg).getRight()));
            return Task.done();
        }

        @Override
        public Task close(final HandlerContext ctx) throws Exception
        {
            session.close();
            return Task.done();
        }
    }
}