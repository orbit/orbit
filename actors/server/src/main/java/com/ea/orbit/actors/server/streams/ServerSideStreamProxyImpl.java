package com.ea.orbit.actors.server.streams;


import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.peer.streams.ClientSideStreamProxy;
import com.ea.orbit.actors.peer.streams.ServerSideStreamProxy;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.server.ServerPeer;
import com.ea.orbit.actors.streams.AsyncObserver;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.streams.simple.StreamReference;
import com.ea.orbit.actors.transactions.IdUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ea.orbit.async.Await.await;

public class ServerSideStreamProxyImpl implements ServerSideStreamProxy, Startable
{
    private Stage stage;
    private ConcurrentMap<StreamSubscriptionHandle, SubscriptionInfo> handleMap = new ConcurrentHashMap<>();
    private ServerPeer peer;

    @Override
    @SuppressWarnings("unchecked")
    public <T> Task<StreamSubscriptionHandle<T>> subscribe(final String provider, final int dataClassId, final String streamId, final ClientSideStreamProxy proxy)
    {
        AsyncStream<?> stream = stage.getStream(provider, DefaultClassDictionary.get().getClassById(dataClassId), streamId);

        Task<? extends StreamSubscriptionHandle<?>> subscription = stream.subscribe(new AsyncObserver()
        {
            @Override
            public Task<Void> onNext(final Object data)
            {
                return proxy.onNext(provider, streamId, data);
            }
        });
        await(subscription);
        final StreamReference.SimpleStreamHandle<T> handle = new StreamReference.SimpleStreamHandle<>(String.valueOf(IdUtils.sequentialLongId()));
        handleMap.putIfAbsent(handle, new SubscriptionInfo(stream, subscription.join(), proxy));
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

        return subscriptionInfo.stream.unSubscribe(subscriptionInfo.actualHandle);
    }

    @Override
    public Task<?> start()
    {
        peer.registerObserver(ServerSideStreamProxy.class, "0", this);
        return Task.done();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<?> stop()
    {
        return Task.allOf(handleMap.values().stream().map(s -> s.stream.unSubscribe(s.actualHandle)));
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
