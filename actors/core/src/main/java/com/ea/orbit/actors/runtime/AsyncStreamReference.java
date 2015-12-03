package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.streams.AsyncObserver;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSequenceToken;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.concurrent.Task;

import java.io.Serializable;

/**
 * Indirection to allow streams to be send from one node to another.
 *
 * @param <T>
 */
public class AsyncStreamReference<T> implements AsyncStream<T>, Serializable
{
    private String provider;
    private Class<T> dataClass;
    private String streamId;
    transient AsyncStream<T> actualStream;
    transient BasicRuntime runtime;

    public AsyncStreamReference()
    {

    }

    public AsyncStreamReference(final String provider, final Class<T> dataClass, final String streamId)
    {
        this.provider = provider;
        this.dataClass = dataClass;
        this.streamId = streamId;
    }

    public AsyncStreamReference(final String provider, final Class<T> dataClass, final String streamId, final AsyncStream<T> actualStream)
    {
        this.provider = provider;
        this.dataClass = dataClass;
        this.streamId = streamId;
        this.actualStream = actualStream;
    }

    public Task<Void> unsubscribe(StreamSubscriptionHandle<T> handle)
    {
        ensureStream();
        return actualStream.unsubscribe(handle);
    }


    public Task<StreamSubscriptionHandle<T>> subscribe(AsyncObserver<T> observer, StreamSequenceToken token)
    {
        ensureStream();
        return actualStream.subscribe(observer, token);
    }

    public Task<Void> publish(T data)
    {
        ensureStream();
        return actualStream.publish(data);
    }


    private void ensureStream()
    {
        if (actualStream == null)
        {
            if (runtime == null)
            {
                runtime = BasicRuntime.getRuntime();
            }
            if (runtime == null)
            {
                throw new IllegalStateException("Can't find the actor runtime");
            }
            AsyncStream<T> stream = runtime.getStream(provider, dataClass, streamId);
            if (stream instanceof AsyncStreamReference)
            {
                actualStream = ((AsyncStreamReference<T>) stream).actualStream;
            }
            else
            {
                actualStream = stream;
            }
        }
    }
}
