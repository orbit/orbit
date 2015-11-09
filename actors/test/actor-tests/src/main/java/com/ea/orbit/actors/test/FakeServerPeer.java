package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.Peer;

import java.nio.ByteBuffer;

public class FakeServerPeer
{
    private Peer peer = new Peer()
    {
        @Override
        protected void sendBinary(final ByteBuffer wrap)
        {
            client.doReceive(wrap);
        }
    };

    private FakeClient client;

    public FakeServerPeer(Stage stage, MessageSerializer serializer)
    {
        peer.setRuntime(stage.getRuntime());
        peer.setSerializer(serializer);
    }


    void onMessage(final ByteBuffer wrap)
    {
        peer.onMessage(wrap);
    }

    public void setClient(final FakeClient client)
    {
        this.client = client;
    }
}
