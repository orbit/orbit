package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.streams.AsyncObserver;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.transactions.IdUtils;
import com.ea.orbit.concurrent.Task;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ea.orbit.async.Await.await;

public class SimpleStreamProxyObject<T> implements SimpleStreamProxy<T>
{
    private Map<String, AsyncObserver<T>> observerMap = new LinkedHashMap<>();
    private volatile Task<String> sharedHandle;
    private final Object mutex = new Object();
    private SimpleStreamExtension provider;
    private SimpleStream streamActorRef;

    public SimpleStreamProxyObject(final SimpleStreamExtension provider, final SimpleStream streamActorRef)
    {
        this.provider = provider;
        this.streamActorRef = streamActorRef;
    }

    @Override
    public Task<Void> onNext(final T data)
    {
        await(Task.allOf(observerMap.values().stream()
                .map(v -> {
                    try
                    {
                        return v.onNext(data);
                    }
                    catch (Throwable ex)
                    {
                        return Task.<Void>fromException(ex);
                    }
                })));
        return Task.done();
    }

    @Override
    public Task<Void> onError(final Exception ex)
    {
        await(Task.allOf(observerMap.values().stream()
                .map(v -> {
                    try
                    {
                        return v.onError(ex);
                    }
                    catch (Throwable ex2)
                    {
                        return Task.<Void>fromException(ex2);
                    }
                })));
        return Task.done();
    }

    public Task<StreamSubscriptionHandle<T>> subscribe(final AsyncObserver<T> observer)
    {
        String handle = IdUtils.urlSafeString(128);
        provider.getHardRefs().add(this);
        observerMap.put(handle, observer);
        if (sharedHandle == null)
        {
            synchronized (mutex)
            {
                if (sharedHandle == null)
                {
                    sharedHandle = streamActorRef.subscribe(this);
                }
            }
        }
        await(sharedHandle);
        return Task.fromValue(new SimpleHandle<>(handle));
    }

    public Task<Void> unsubscribe(String handle)
    {
        observerMap.remove(handle);
        if (sharedHandle != null && observerMap.size() == 0)
        {
            // await(sharedHandle);
            // TODO unSubscribe from the stream
            provider.getHardRefs().remove(this);
        }
        return Task.done();
    }
}
