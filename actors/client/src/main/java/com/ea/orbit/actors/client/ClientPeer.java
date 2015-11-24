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

package com.ea.orbit.actors.client;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Invocation;
import com.ea.orbit.actors.runtime.Messaging;
import com.ea.orbit.actors.runtime.ObjectInvoker;
import com.ea.orbit.actors.runtime.Peer;
import com.ea.orbit.actors.runtime.RemoteClient;
import com.ea.orbit.actors.runtime.RemoteReference;
import com.ea.orbit.actors.runtime.SerializationHandler;
import com.ea.orbit.actors.streams.AsyncObserver;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;

import java.lang.reflect.Method;

import static com.ea.orbit.async.Await.await;

/**
 * This works as a bridge to perform calls between the server and a client.
 */
public class ClientPeer extends Peer implements BasicRuntime, Startable, RemoteClient
{

    private Messaging messaging;

    public Task<Void> cleanup()
    {
        await(messaging.cleanup());
        return Task.done();
    }

    @Override
    public Task<?> start()
    {
        getPipeline().addLast(DefaultHandlers.EXECUTION, new ClientPeerExecutor());
        messaging = new Messaging();
        messaging.setRuntime(this);
        getPipeline().addLast(DefaultHandlers.MESSAGING, messaging);
        getPipeline().addLast(DefaultHandlers.SERIALIZATION, new SerializationHandler(this, getMessageSerializer()));
        getPipeline().addLast(DefaultHandlers.NETWORK, getNetwork());
        return getPipeline().connect(null);
    }

    @Override
    public Task<?> invoke(final RemoteReference toReference, final Method m, final boolean oneWay, final int methodId, final Object[] params)
    {
        final Invocation invocation = new Invocation(toReference, m, oneWay, methodId, params, null);
        return getPipeline().write(invocation);
    }

    @Override
    public <T extends ActorObserver> T registerObserver(final Class<T> iClass, final T observer)
    {
        return null;
    }

    @Override
    public <T extends ActorObserver> T registerObserver(final Class<T> iClass, final String id, final T observer)
    {
        return null;
    }

    @Override
    public <T extends com.ea.orbit.actors.ActorObserver> T getRemoteObserverReference(final NodeAddress address, final Class<T> iClass, final Object id)
    {
        return null;
    }

    @Override
    public <T extends Actor> T getReference(final Class<T> iClass, final Object id)
    {
        return DefaultDescriptorFactory.get().getReference(this, null, iClass, id);
    }

    @Override
    public ObjectInvoker<?> getInvoker(final int interfaceId)
    {
        return null;
    }

    @Override
    public <T> AsyncStream<T> getStream(final String provider, final Class<T> dataClass, final String id)
    {
        return new AsyncStream<T>()
        {
            @Override
            public Task<Void> unSubscribe(final StreamSubscriptionHandle<T> handle)
            {
                return null;
            }

            @Override
            public Task<StreamSubscriptionHandle<T>> subscribe(final AsyncObserver<T> observer)
            {
                return null;
            }

            @Override
            public Task<Void> post(final T data)
            {
                return null;
            }
        };
    }
}
