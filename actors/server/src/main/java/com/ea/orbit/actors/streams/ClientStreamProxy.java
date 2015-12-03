package com.ea.orbit.actors.streams;


import com.ea.orbit.actors.runtime.ActorRuntime;
import com.ea.orbit.actors.runtime.Invocation;
import com.ea.orbit.actors.server.ServerPeer;
import com.ea.orbit.actors.transactions.IdUtils;
import com.ea.orbit.concurrent.Task;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ea.orbit.async.Await.await;

public class ClientStreamProxy
{
    private Map<String, StreamSubscription> observerMap = new LinkedHashMap<>();
    ActorRuntime runtime;
    ServerPeer peer;

    static class StreamSubscription
    {
        String provider;
        Class dataClass;
        String streamId;
        boolean valid;
        Task<StreamSubscriptionHandle> handle;
    }

    @SuppressWarnings("unchecked")
    public Task<String> subscribe(String provider, Class dataClass, String streamId)
    {
        String handle = IdUtils.urlSafeString(128);
        StreamSubscription subscription = new StreamSubscription();
        subscription.provider = provider;
        subscription.dataClass = dataClass;
        subscription.streamId = streamId;
        subscription.handle = runtime.getStream(provider, dataClass, streamId).subscribe(new AsyncObserver()
        {
            @Override
            public Task<Void> onNext(final Object data, final StreamSequenceToken sequenceToken)
            {
                if (subscription.valid && peer.getPipeline().isActive())
                {
                    return peer.getPipeline().write(new Invocation());
                }
                return Task.done();
            }
        });
        observerMap.put(handle, subscription);
        await(subscription.handle);
        return Task.fromValue(handle);
    }

    @SuppressWarnings("unchecked")
    public Task<Void> unsubscribe(String handle)
    {
        StreamSubscription subscription = observerMap.remove(handle);
        // no more messages
        if (subscription != null && subscription.valid)
        {
            subscription.valid = false;
            await(subscription.handle);
            await(runtime.getStream(subscription.provider, subscription.dataClass, subscription.streamId)
                    .unsubscribe(subscription.handle.join()));
            observerMap.remove(handle);
        }
        return Task.done();
    }

}
