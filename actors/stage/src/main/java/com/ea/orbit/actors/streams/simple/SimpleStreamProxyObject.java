package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.streams.AsyncObserver;
import com.ea.orbit.actors.streams.StreamSequenceToken;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.transactions.IdUtils;
import com.ea.orbit.concurrent.Task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ea.async.Async.await;

public class SimpleStreamProxyObject<T> implements SimpleStreamProxy<T>
{
    private Map<String, AsyncObserver<T>> observerMap = new ConcurrentHashMap<>();
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
    public Task<Void> onNext(final T data, final StreamSequenceToken sequenceToken)
    {
        await(Task.allOf(observerMap.values().stream()
                .map(v -> {
                    try
                    {
                        return v.onNext(data, null);
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
        observerMap.put(handle, observer);
        synchronized (mutex)
        {
            if (sharedHandle == null)
            {
                sharedHandle = streamActorRef.subscribe(this);
                provider.getHardRefs().add(this);
            }
        }
        await(sharedHandle);
        return Task.fromValue(new SimpleHandle<>(handle));
    }

    public Task<Void> unsubscribe(String handle)
    {
        observerMap.remove(handle);
        if (sharedHandle != null)
        {
            Task<String> sharedHandle = this.sharedHandle;
            final String actualHandle = await(sharedHandle);
            synchronized (mutex)
            {
                if (this.sharedHandle == sharedHandle && observerMap.size() == 0)
                {
                    this.sharedHandle = null;
                    provider.getHardRefs().remove(this);
                    // unsubscribe from the stream
                    return streamActorRef.unsubscribe(actualHandle);
                }
            }
        }
        return Task.done();
    }
}
