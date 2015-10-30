package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.Peer;

import java.nio.ByteBuffer;

public class FakeClient implements RemoteClient
{
    private final FakeServerPeer server;
    private Peer peer = new Peer() {
        @Override
        protected void sendBinary(final ByteBuffer wrap)
        {
            doSendBinary(wrap);
        }
    };

    public FakeClient(InvokerProvider invokerProvider, MessageSerializer serializer, FakeServerPeer server)
    {
        this.server = server;
        peer.setRuntime(null);
        peer.setSerializer(serializer);
    }

    @Override
    public void cleanup(final boolean b)
    {

    }

    private void doSendBinary(final ByteBuffer wrap)
    {
        server.onMessage(wrap);
    }

    void doReceive(final ByteBuffer wrap)
    {
        peer.onMessage(wrap);
    }

    @Override
    public <T extends Actor> T getReference(final Class<T> iClass, final Object id)
    {
        return peer.getReference(iClass, (String) id);
    }
}
