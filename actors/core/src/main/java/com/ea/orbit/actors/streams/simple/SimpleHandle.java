package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.streams.StreamSubscriptionHandle;

import java.io.Serializable;

public class SimpleHandle<T> implements StreamSubscriptionHandle<T>, Serializable
{
    private String id;

    public SimpleHandle()
    {
    }

    public SimpleHandle(final String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
}
