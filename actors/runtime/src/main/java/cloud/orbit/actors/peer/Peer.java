/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.actors.peer;

import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.extensions.PipelineExtension;
import cloud.orbit.actors.net.DefaultPipeline;
import cloud.orbit.actors.net.Handler;
import cloud.orbit.actors.net.Pipeline;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.LocalObjects;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.util.IdUtils;
import cloud.orbit.concurrent.Task;
import cloud.orbit.lifecycle.Startable;
import cloud.orbit.exception.NotImplementedException;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * This works as a bridge to perform calls between the server and a client.
 */
public abstract class Peer implements Startable, BasicRuntime
{
    protected LocalObjects objects = new LocalObjects();
    private Pipeline pipeline = new DefaultPipeline(this);
    private Clock clock = Clock.systemUTC();
    private MessageSerializer messageSerializer;
    private Handler network;
    private List<PeerExtension> extensions = new ArrayList<>();
    protected final String localIdentity = String.valueOf(IdUtils.sequentialLongId());
    private final WeakReference<BasicRuntime> cachedRef = new WeakReference<>(this);

    public void setNetworkHandler(Handler network)
    {
        this.network = network;
    }

    public Handler getNetwork()
    {
        return network;
    }

    public void setMessageSerializer(final MessageSerializer messageSerializer)
    {
        this.messageSerializer = messageSerializer;
    }

    public Clock clock()
    {
        return clock;
    }

    public MessageSerializer getMessageSerializer()
    {
        return messageSerializer;
    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }


    public Pipeline getPipeline()
    {
        return pipeline;
    }


    @Override
    public Task<?> invoke(final RemoteReference toReference, final Method m, final boolean oneWay, final int methodId, final Object[] params)
    {
        final Invocation invocation = new Invocation(toReference, m, oneWay, methodId, params, null);
        return getPipeline().write(invocation);
    }

    @Override
    public <T extends ActorObserver> T registerObserver(final Class<T> iClass, final String id, final T observer)
    {
        final RemoteReference<T> reference = objects.getOrAddLocalObjectReference(null, iClass, id, observer);
        RemoteReference.setRuntime(reference, this);
        //noinspection unchecked
        return iClass != null ? iClass.cast(reference) : (T) reference;
    }

    @Override
    public <T> AsyncStream<T> getStream(final String provider, final Class<T> dataClass, final String id)
    {
        throw new NotImplementedException("getStream");
    }

    protected void installPipelineExtensions()
    {
        extensions.stream().filter(extension -> extension instanceof PipelineExtension)
                .map(extension -> (PipelineExtension) extension)
                .forEach(extension -> {
                    if (extension.getBeforeHandlerName() != null)
                    {
                        pipeline.addHandlerBefore(extension.getBeforeHandlerName(), extension.getName(), extension);
                    }
                    else if (extension.getAfterHandlerName() != null)
                    {
                        pipeline.addHandlerAfter(extension.getAfterHandlerName(), extension.getName(), extension);
                    }
                    else
                    {
                        pipeline.addFirst(extension.getName(), extension);
                    }
                });
    }

    @Override
    public <T> T getReference(final BasicRuntime runtime, final NodeAddress address, final Class<T> iClass, final Object id)
    {
        return DefaultDescriptorFactory.get().getReference(this, address, iClass, id);
    }

    public List<PeerExtension> getExtensions()
    {
        return extensions;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getFirstExtension(Class<T> itemType)
    {
        return extensions == null ? null :
                (T) extensions.stream().filter(p -> itemType.isInstance(p)).findFirst().orElse(null);

    }

    public void addExtension(PeerExtension extension)
    {
        this.extensions.add(extension);
    }

    public void bind()
    {
        BasicRuntime.setRuntime(cachedRef);
    }
}
