package com.ea.orbit.actors.streams;

import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.concurrent.Task;

public interface AsyncStream<T>
{
    String DEFAULT_PROVIDER = "default";

    Task<Void> unsubscribe(StreamSubscriptionHandle<T> handle);

    Task<StreamSubscriptionHandle<T>> subscribe(AsyncObserver<T> observer);

    Task<Void> publish(T data);


    static <DATA> AsyncStream<DATA> getStream(Class<DATA> dataClass, String id)
    {
        return getStream(dataClass, DEFAULT_PROVIDER, id);
    }

    static <DATA> AsyncStream<DATA> getStream(Class<DATA> dataClass, String provider, String streamId)
    {
        return BasicRuntime.getRuntime().getStream(provider, dataClass, streamId);
    }
}
