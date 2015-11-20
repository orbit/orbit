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

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.client.ClientPeer;
import com.ea.orbit.actors.runtime.Peer;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import java.net.URI;

@ClientEndpoint
public class WebSocketClient extends AbstractWebSocket
{
    private ClientPeer peer = new ClientPeer();

    public ClientPeer getPeer()
    {
        return peer;
    }

    @Override
    protected Peer peer()
    {
        return peer;
    }

    public Task connect(final URI endpointURI)
    {
        final WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
        try
        {
            session = wsContainer.connectToServer(this, endpointURI);
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
        return Task.done();
    }

    public <T extends Actor> T getReference(final Class<T> iClass, final Object id)
    {
        return peer.getReference(iClass, id);
    }

    /**
     * Register a object locally without notifying the cluster about it's location.
     *
     * @param remoteInterface the implemented remote interface class (T.class)
     * @param implementation  a implementation of the remote interface
     * @param <T>             the remote interface type
     */

    public <T> void registerLocalObject(final Class<T> remoteInterface, final T interfaceImplementation)
    {

    }
}
