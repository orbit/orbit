package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.streams.AsyncObserver;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.concurrent.Task;

import java.io.Serializable;

public class StreamReference<T> implements AsyncStream<T>
{
    private final SimpleStreamExtension provider;
    private final Class<T> dataClass;
    private final SimpleStream streamActorRef;
    SimpleStreamProxyObject<T> subscriberObject;

    public static class SimpleStreamHandle<T> implements StreamSubscriptionHandle<T>, Serializable
    {
        private String id;

        public SimpleStreamHandle()
        {
        }

        public SimpleStreamHandle(final String id)
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }
    }


    public StreamReference(SimpleStreamExtension provider, final Class<T> dataClass, final String id)
    {
        this.provider = provider;
        this.dataClass = dataClass;
        final int dataClassId = DefaultClassDictionary.get().getClassId(dataClass);
        final String oid = dataClassId + ":" + id;

        //noinspection unchecked
        this.streamActorRef = (SimpleStream) Actor.getReference(SimpleStream.class, oid);
    }

    @Override
    public Task<Void> unsubscribe(final StreamSubscriptionHandle<T> handle)
    {
        SimpleStreamProxyObject<Object> subscriber = provider.getSubscriber(streamActorRef);
        return subscriber.unsubscribe(((SimpleHandle<T>) handle).getId());
    }

    @Override
    public Task<StreamSubscriptionHandle<T>> subscribe(final AsyncObserver<T> observer)
    {

        SimpleStreamProxyObject<T> subscriber = provider.getSubscriber(streamActorRef);
        return subscriber.subscribe(observer);
    }

    @Override
    public Task<Void> publish(final T data)
    {
        return streamActorRef.publish(data);
    }

}
