package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.extensions.ActorExtension;
import com.ea.orbit.actors.extensions.StreamProvider;
import com.ea.orbit.actors.streams.AsyncStream;

public class SimpleStreamExtension implements ActorExtension, StreamProvider
{
    private String name;


    public SimpleStreamExtension()
    {
    }

    public SimpleStreamExtension(final String name)
    {
        this.name = name;
    }

    @Override
    public <T> AsyncStream<T> getStream(final Class<T> dataClass, final String id)
    {
        return new StreamReference<>(this, dataClass, id);
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public <T> SimpleStreamProxyObject<T> getSubscriber(final SimpleStream streamActorRef)
    {
        // TODO: cache, very important
        return new SimpleStreamProxyObject<>(streamActorRef);
    }
}
