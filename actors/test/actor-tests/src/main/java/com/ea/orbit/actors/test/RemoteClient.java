package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;

public interface RemoteClient
{
    void cleanup(boolean b);

    <T extends Actor> T getReference(final Class<T> iClass, final Object id);
}
