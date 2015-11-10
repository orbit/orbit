package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.Peer;

public class FakeServerPeer extends Peer
{
    public FakeServerPeer(Stage stage, MessageSerializer serializer)
    {
        setRuntime(stage.getRuntime());
        setSerializer(serializer);
    }

}
