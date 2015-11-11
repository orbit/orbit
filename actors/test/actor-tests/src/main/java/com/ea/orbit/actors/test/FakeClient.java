package com.ea.orbit.actors.test;

import com.ea.orbit.actors.runtime.Peer;

public class FakeClient extends Peer implements RemoteClient
{
    public FakeClient()
    {
    }

    @Override
    public void cleanup(final boolean b)
    {

    }

    @Override
    public <T> T getReference(final Class<T> iClass, final String id)
    {
        return null;
    }

}
