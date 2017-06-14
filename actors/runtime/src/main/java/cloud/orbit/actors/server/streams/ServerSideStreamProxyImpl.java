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

package cloud.orbit.actors.server.streams;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.peer.streams.ClientSideStreamProxy;
import cloud.orbit.actors.peer.streams.ServerSideStreamProxy;
import cloud.orbit.actors.runtime.DefaultClassDictionary;
import cloud.orbit.actors.server.ServerPeer;
import cloud.orbit.actors.streams.AsyncObserver;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSequenceToken;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.actors.streams.simple.StreamReference;
import cloud.orbit.actors.util.IdUtils;
import cloud.orbit.concurrent.Task;
import cloud.orbit.lifecycle.Startable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ea.async.Async.await;

public class ServerSideStreamProxyImpl implements ServerSideStreamProxy, Startable
{
    private Logger logger = LoggerFactory.getLogger(ServerSideStreamProxyImpl.class);
    private Stage stage;
    private ConcurrentMap<StreamSubscriptionHandle, SubscriptionInfo> handleMap = new ConcurrentHashMap<>();
    private ServerPeer peer;

    @Override
    @SuppressWarnings("unchecked")
    public <T> Task<StreamSubscriptionHandle<T>> subscribe(final String provider, final int dataClassId, final String streamId, final ClientSideStreamProxy proxy)
    {
        logger.debug("Subscribing server side stream proxy.");
        Class<?> dataClass = DefaultClassDictionary.get().getClassById(dataClassId);
        AsyncStream<?> stream = stage.getStream(provider, dataClass, streamId);

        Task<? extends StreamSubscriptionHandle<?>> subscription = stream.subscribe(new AsyncObserver()
        {
            @Override
            public Task<Void> onNext(final Object data, final StreamSequenceToken sequenceToken)
            {
                return proxy.onNext(provider, streamId, data);
            }
        }, null);
        await(subscription);
        final StreamReference.SimpleStreamHandle<T> handle = new StreamReference.SimpleStreamHandle<>(String.valueOf(IdUtils.sequentialLongId()));
        handleMap.putIfAbsent(handle, new SubscriptionInfo(stream, subscription.join(), proxy));
        logger.debug("Successfully subscribed server stream proxy.");
        return Task.fromValue(handle);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Task<Void> unsubscribe(final StreamSubscriptionHandle<T> handle)
    {
        final SubscriptionInfo subscriptionInfo = handleMap.remove(handle);
        if (subscriptionInfo == null)
        {
            throw new IllegalArgumentException("Not subscribed " + handle);
        }
        logger.debug("Unsubscribed server stream proxy.");
        return subscriptionInfo.stream.unsubscribe(subscriptionInfo.actualHandle);
    }

    @Override
    public Task<?> start()
    {
        logger.info("Starting server stream proxy.");
        peer.registerObserver(ServerSideStreamProxy.class, "0", this);
        return Task.done();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<?> stop()
    {
        logger.info("Stopping server stream proxy.");
        return Task.allOf(handleMap.values().stream().map(s -> s.stream.unsubscribe(s.actualHandle)));
    }

    public void setStage(final Stage stage)
    {
        this.stage = stage;
    }

    public void setPeer(final ServerPeer peer)
    {
        this.peer = peer;
    }
}

class SubscriptionInfo
{
    AsyncStream stream;
    StreamSubscriptionHandle actualHandle;
    ClientSideStreamProxy proxy;

    public SubscriptionInfo(final AsyncStream stream, final StreamSubscriptionHandle subscription, final ClientSideStreamProxy proxy)
    {
        this.stream = stream;
        this.actualHandle = subscription;
        this.proxy = proxy;
    }
}
