package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.extensions.ActorExtension;
import com.ea.orbit.actors.extensions.StreamProvider;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.concurrent.ConcurrentHashSet;
import com.ea.orbit.exception.UncheckedException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutionException;

public class SimpleStreamExtension implements ActorExtension, StreamProvider
{
    private String name;
    private Cache<Actor, SimpleStreamProxyObject> weakCache = CacheBuilder.newBuilder()
            .weakValues()
            .build();

    private ConcurrentHashSet<SimpleStreamProxyObject> hardRefs = new ConcurrentHashSet<>();


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
        try
        {
            //noinspection unchecked
            return weakCache.get(streamActorRef, () -> new SimpleStreamProxyObject<T>(this, streamActorRef));
        }
        catch (ExecutionException e)
        {
            throw new UncheckedException(e);
        }
    }

    ConcurrentHashSet<SimpleStreamProxyObject> getHardRefs()
    {
        return hardRefs;
    }
}
