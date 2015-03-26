/*
Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

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

package com.ea.orbit.actors;


import com.ea.orbit.actors.cluster.ClusterPeer;
import com.ea.orbit.actors.cluster.IClusterPeer;
import com.ea.orbit.actors.providers.ILifetimeProvider;
import com.ea.orbit.actors.providers.IOrbitProvider;
import com.ea.orbit.actors.runtime.Execution;
import com.ea.orbit.actors.runtime.Hosting;
import com.ea.orbit.actors.runtime.IHosting;
import com.ea.orbit.actors.runtime.Messaging;
import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.OrbitContainer;
import com.ea.orbit.container.Startable;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Singleton
public class OrbitStage implements Startable
{
    private IClusterPeer clusterPeer;
    @Config("orbit.actors.clusterName")
    private String clusterName;
    private Task startFuture;

    private Messaging messaging;
    private Execution execution;
    private Hosting hosting;
    private StageMode mode = StageMode.HOST;
    private boolean startCalled;
    private Clock clock;
    private ExecutorService executionPool;
    private ExecutorService messagingPool;

    @Inject
    OrbitContainer orbitContainer;  // Only injected if running on Orbit container

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public void setExecutionPool(final ExecutorService executionPool)
    {
        this.executionPool = executionPool;
    }

    public ExecutorService getExecutionPool()
    {
        return executionPool;
    }

    public void setMessagingPool(final ExecutorService messagingPool)
    {
        this.messagingPool = messagingPool;
    }

    public ExecutorService getMessagingPool()
    {
        return messagingPool;
    }

    public enum StageMode
    {
        FRONT_END, // no activations
        HOST // allows activations
    }

    private List<IOrbitProvider> providers = new ArrayList<>();

    public String getClusterName()
    {
        return clusterName;
    }

    public void setClusterName(final String clusterName)
    {
        this.clusterName = clusterName;
    }

    public StageMode getMode()
    {
        return mode;
    }

    public void setMode(final StageMode mode)
    {
        if (startCalled)
        {
            throw new IllegalStateException("Stage mode cannot be changed after startup.");
        }
        this.mode = mode;
    }

    public Task start()
    {
        startCalled = true;

        if (hosting == null)
        {
            hosting = new Hosting();
        }
        if (messaging == null)
        {
            messaging = new Messaging();
        }
        if (execution == null)
        {
            execution = new Execution();
        }
        if (clusterPeer == null)
        {
            clusterPeer = new ClusterPeer();
        }
        if (clock == null)
        {
            clock = Clock.systemUTC();
        }

        this.wireOrbitContainer();

        hosting.setNodeType(mode == StageMode.HOST ? IHosting.NodeTypeEnum.SERVER : IHosting.NodeTypeEnum.CLIENT);
        execution.setClock(clock);
        execution.setHosting(hosting);
        execution.setMessaging(messaging);
        execution.setExecutor(executionPool);

        messaging.setExecution(execution);
        messaging.setClock(clock);
        messaging.setExecutor(messagingPool);

        hosting.setExecution(execution);
        hosting.setClusterPeer(clusterPeer);
        messaging.setClusterPeer(clusterPeer);
        execution.setProviders(providers);

        messaging.start();
        hosting.start();
        execution.start();
        startFuture = clusterPeer.join(clusterName);
        // todo remove this
        startFuture.join();
        return startFuture;
    }

    private void wireOrbitContainer()
    {
        // orbitContainer will be null if the application is not using it
        if(orbitContainer != null)
        {
            ILifetimeProvider containerLifetime = new ILifetimeProvider()
            {
                @Override
                public Task preActivation(OrbitActor actor)
                {
                    orbitContainer.inject(actor);
                    return Task.done();
                }
            };

            providers.add(containerLifetime);
        }
    }

    @SuppressWarnings({"unsafe", "unchecked"})
    public <T extends IActor> T getReference(final Class<T> iClass, final String id)
    {
        return execution.getReference(iClass, id);
    }

    public void setClusterPeer(final IClusterPeer clusterPeer)
    {
        this.clusterPeer = clusterPeer;
    }

    public void addProvider(final IOrbitProvider provider)
    {
        this.providers.add(provider);
    }

    public Task stop()
    {
        return execution.stop()
                .thenRun(clusterPeer::leave);
    }

    public <T extends IActorObserver> T getObserverReference(Class<T> iClass, final T observer)
    {
        return execution.getObjectReference(iClass, observer);
    }

    public <T extends IActorObserver> T getObserverReference(final T observer)
    {
        return execution.getObjectReference(null, observer);
    }

    public Hosting getHosting()
    {
        return hosting;
    }

    public IClusterPeer getClusterPeer()
    {
        return clusterPeer != null ? clusterPeer : (clusterPeer = new ClusterPeer());
    }

    public void cleanup(boolean block)
    {
        execution.activationCleanup(block);
        messaging.timeoutCleanup();
    }
}
