package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.Peer;

import java.nio.ByteBuffer;

public class FakeClient extends Peer implements RemoteClient
{
    public FakeClient(InvokerProvider invokerProvider, MessageSerializer serializer)
    {
        setSerializer(serializer);
    }

    @Override
    public void cleanup(final boolean b)
    {

    }


    @Override
    public <T> T getReference(final Class<T> iClass, final String id)
    {
        return super.getReference(iClass, (String) id);
    }
}
