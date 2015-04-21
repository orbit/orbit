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
import com.ea.orbit.actors.runtime.IReminderController;
import com.ea.orbit.actors.runtime.Messaging;
import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.annotation.Wired;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.OrbitContainer;
import com.ea.orbit.container.Startable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Singleton
public class OrbitStage implements Startable
{
    private static final Logger logger = LoggerFactory.getLogger(OrbitStage.class);

    @Config("orbit.actors.clusterName")
    private String clusterName;

    @Config("orbit.actors.nodeName")
    private String nodeName;

    @Config("orbit.actors.stageMode")
    private StageMode mode = StageMode.HOST;

    @Config("orbit.actors.providers")
    private List<IOrbitProvider> providers = new ArrayList<>();

    @Wired
    OrbitContainer orbitContainer;

    public enum StageMode
    {
        FRONT_END, // no activations
        HOST // allows activations
    }

    private IClusterPeer clusterPeer;
    private Task<?> startFuture;
    private Messaging messaging;
    private Execution execution;
    private Hosting hosting;
    private boolean startCalled;
    private Clock clock;
    private ExecutorService executionPool;
    private ExecutorService messagingPool;

    static
    {
        // Initializes orbit async, but only if the application uses it.
        try
        {
            Class.forName("com.ea.orbit.async.Async");
            try
            {
                // async is present in the classpath, let's make sure await is initialized
                Class.forName("com.ea.orbit.async.Await");
            }
            catch (Exception ex)
            {
                // this might be a problem, logging.
                logger.error("Error initializing orbit-async", ex);
            }
        }
        catch (Exception ex)
        {
            // no problem, application doesn't use orbit async.
        }
    }

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

    public String runtimeIdentity()
    {
        if (execution == null)
        {
            throw new IllegalStateException("Can only be called after the startup");
        }
        return execution.runtimeIdentity();
    }

    public String getClusterName()
    {
        return clusterName;
    }

    public void setClusterName(final String clusterName)
    {
        this.clusterName = clusterName;
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(final String nodeName)
    {
        this.nodeName = nodeName;
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

    public Task<?> start()
    {
        startCalled = true;

        if (clusterName == null || clusterName.isEmpty())
        {
            setClusterName("orbit-cluster");
        }

        if (nodeName == null || nodeName.isEmpty())
        {
            setNodeName(getClusterName());
        }

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

        this.configureOrbitContainer();

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

        execution.setOrbitProviders(providers);

        messaging.start();
        hosting.start();
        execution.start();


        Task<?> future = clusterPeer.join(clusterName, nodeName);
        if (mode == StageMode.HOST)
        {
            future = future.thenRun(() -> IActor.getReference(IReminderController.class, "0").ensureStart());
        }
        startFuture = future;

        startFuture.join();
        bind();

        return startFuture;
    }

    private void configureOrbitContainer()
    {
        // orbitContainer will be null if the application is not using it
        if (orbitContainer != null)
        {
            // Create a lifetime provider for actor DI
            ILifetimeProvider containerLifetime = new ILifetimeProvider()
            {
                @Override
                public Task<?> preActivation(OrbitActor<?> actor)
                {
                    orbitContainer.inject(actor);
                    return Task.done();
                }
            };

            providers.add(containerLifetime);
        }
    }

    public void setClusterPeer(final IClusterPeer clusterPeer)
    {
        this.clusterPeer = clusterPeer;
    }

    /**
     * Installs extensions to the stage.
     * <p/>
     * Example:
     * <pre>
     * stage.addProvider(new MongoDbProvider(...));
     * </pre>
     *
     * @param provider Actor Provider instance.
     */
    public void addProvider(final IOrbitProvider provider)
    {
        this.providers.add(provider);
    }

    public Task<?> stop()
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

    /**
     * Binds this stage to the current thread.
     * This tells ungrounded references to use this stage to call remote methods.
     * <p/>
     * An ungrounded reference is a reference created with {@code IActor.getReference} and used outside of an actor method.
     * <p/>
     * This is only necessary when there are <i>two or more</i> OrbitStages active in the same virtual machine and
     * remote calls need to be issued from outside an actor.
     * This method was created to help with test cases.
     * <p/>
     * A normal application will have a single stage and should have no reason to call this method.
     * <p/>
     * This method writes a weak reference to the runtime in a thread local.
     * No cleanup is necessary, so none is available.
     */
    public void bind()
    {
        execution.bind();
    }


}
