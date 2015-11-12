package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.net.DefaultPipeline;
import com.ea.orbit.actors.net.Handler;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.Invocation;
import com.ea.orbit.actors.runtime.ObjectInvoker;
import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Messaging;
import com.ea.orbit.actors.runtime.Peer;
import com.ea.orbit.actors.runtime.SerializationHandler;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;

import java.lang.reflect.Method;

public class FakeClient extends Peer implements RemoteClient, Startable, BasicRuntime
{
    private final DefaultPipeline pipeline;

    public FakeClient(MessageSerializer serializer, Handler network)
    {
        pipeline = new DefaultPipeline();
        pipeline.addLast(DefaultHandlers.EXECUTION, new ClientPeerExecutor());
        pipeline.addLast(DefaultHandlers.MESSAGING, new Messaging());
        pipeline.addLast(DefaultHandlers.SERIALIZATION, new SerializationHandler(this, serializer));
        pipeline.addLast(DefaultHandlers.NETWORK, network);
    }

    @Override
    public void cleanup(final boolean b)
    {

    }

    @Override
    public Task<?> start()
    {
        return pipeline.connect(null);
    }

    @Override
    public Task<?> invoke(final Addressable toReference, final Method m, final boolean oneWay, final int methodId, final Object[] params)
    {
        final Invocation invocation = new Invocation(toReference, m, oneWay, methodId, params, null);
        return pipeline.write(invocation);
    }

    @Override
    public <T extends ActorObserver> T registerObserver(final Class<T> iClass, final T observer)
    {
        return null;
    }

    @Override
    public <T extends ActorObserver> T getRemoteObserverReference(final NodeAddress address, final Class<T> iClass, final Object id)
    {
        return null;
    }

    @Override
    public <T extends Actor> T getReference(final Class<T> iClass, final Object id)
    {
        return DefaultDescriptorFactory.get().getReference(this, iClass, id);
    }

    @Override
    public ObjectInvoker<?> getInvoker(final int interfaceId)
    {
        return null;
    }

}
