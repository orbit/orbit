package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.net.DefaultPipeline;
import com.ea.orbit.actors.net.Handler;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Messaging;
import com.ea.orbit.actors.runtime.SerializationHandler;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;

public class FakeServerPeer implements Startable
{
    private final DefaultPipeline pipeline;

    public FakeServerPeer(Stage stage, MessageSerializer serializer, Handler network)
    {
        pipeline = new DefaultPipeline();
        pipeline.addLast(DefaultHandlers.EXECUTION, new ServerPeerExecutor(stage));
        pipeline.addLast(DefaultHandlers.MESSAGING, new Messaging());
        pipeline.addLast(DefaultHandlers.SERIALIZATION, new SerializationHandler(stage, serializer));
        pipeline.addLast(DefaultHandlers.NETWORK, network);
    }

    @Override
    public Task<?> start()
    {
        return pipeline.connect(null);
    }
}
