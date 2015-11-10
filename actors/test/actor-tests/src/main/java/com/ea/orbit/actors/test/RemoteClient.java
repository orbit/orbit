package com.ea.orbit.actors.test;

public interface RemoteClient
{
    void cleanup(boolean block);

    <T> T getReference(final Class<T> iClass, final String id);
}
