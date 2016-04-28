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

package cloud.orbit.actors;

import cloud.orbit.actors.annotation.NeverDeactivate;
import cloud.orbit.actors.annotation.StatelessWorker;
import cloud.orbit.actors.cluster.ClusterPeer;
import cloud.orbit.actors.cluster.JGroupsClusterPeer;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.concurrent.MultiExecutionSerializer;
import cloud.orbit.actors.concurrent.WaitFreeMultiExecutionSerializer;
import cloud.orbit.actors.extensions.ActorClassFinder;
import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.actors.extensions.DefaultLoggerExtension;
import cloud.orbit.actors.extensions.LoggerExtension;
import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.extensions.PipelineExtension;
import cloud.orbit.actors.extensions.StreamProvider;
import cloud.orbit.actors.net.DefaultPipeline;
import cloud.orbit.actors.net.Pipeline;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorBaseEntry;
import cloud.orbit.actors.runtime.ActorEntry;
import cloud.orbit.actors.runtime.ActorRuntime;
import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.actors.runtime.AsyncStreamReference;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.ClusterHandler;
import cloud.orbit.actors.runtime.DefaultActorClassFinder;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.Execution;
import cloud.orbit.actors.runtime.Hosting;
import cloud.orbit.actors.runtime.InternalUtils;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.JavaMessageSerializer;
import cloud.orbit.actors.runtime.LocalObjects;
import cloud.orbit.actors.runtime.MessageLoopback;
import cloud.orbit.actors.runtime.Messaging;
import cloud.orbit.actors.runtime.NodeCapabilities;
import cloud.orbit.actors.runtime.ObserverEntry;
import cloud.orbit.actors.runtime.Registration;
import cloud.orbit.actors.runtime.ReminderController;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.actors.runtime.ResponseCaching;
import cloud.orbit.actors.runtime.SerializationHandler;
import cloud.orbit.actors.runtime.StatelessActorEntry;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.actors.cloner.KryoCloner;
import cloud.orbit.actors.cloner.NoOpCloner;
import cloud.orbit.actors.streams.AsyncObserver;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSequenceToken;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.actors.streams.simple.SimpleStreamExtension;
import cloud.orbit.actors.transactions.IdUtils;
import cloud.orbit.actors.transactions.TransactionUtils;
import cloud.orbit.annotation.Config;
import cloud.orbit.concurrent.ExecutorUtils;
import cloud.orbit.concurrent.Task;
import cloud.orbit.lifecycle.Startable;
import cloud.orbit.exception.UncheckedException;
import cloud.orbit.util.StringUtils;

import com.ea.async.Async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.annotation.StorageExtension;

import javax.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import static com.ea.async.Async.await;

@Singleton
public class Stage implements Startable, ActorRuntime
{
    private Logger logger = LoggerFactory.getLogger(Stage.class);

    private static final int DEFAULT_EXECUTION_POOL_SIZE = 128;

    LocalObjects objects = new LocalObjects()
    {
        @Override
        protected <T> LocalObjectEntry createLocalObjectEntry(final RemoteReference<T> reference, final T object)
        {
            return Stage.this.createLocalObjectEntry(reference, object);
        }
    };

    @Config("orbit.actors.clusterName")
    private String clusterName;

    @Config("orbit.actors.nodeName")
    private String nodeName;

    @Config("orbit.actors.stageMode")
    private StageMode mode = StageMode.HOST;

    @Config("orbit.actors.executionPoolSize")
    private int executionPoolSize = DEFAULT_EXECUTION_POOL_SIZE;

    @Config("orbit.actors.extensions")
    private List<ActorExtension> extensions = new CopyOnWriteArrayList<>();

    @Config("orbit.actors.stickyHeaders")
    private Set<String> stickyHeaders = new HashSet<>(Arrays.asList(TransactionUtils.ORBIT_TRANSACTION_ID, "orbit.traceId"));

    @Config("orbit.actors.pulseInterval")
    private long pulseIntervalMillis = TimeUnit.SECONDS.toMillis(10);
    private Timer timer;
    private Pipeline pipeline;

    private final String runtimeIdentity = "Orbit[" + IdUtils.urlSafeString(128) + "]";

    private ResponseCaching cacheManager;

    private MultiExecutionSerializer<Object> executionSerializer;
    private ActorClassFinder finder;
    private LoggerExtension loggerExtension;
    private NodeCapabilities.NodeState state;

    @Config("orbit.actors.concurrentDeactivations")
    private int concurrentDeactivations = 16;

    @Config("orbit.actors.defaultActorTTL")
    private long defaultActorTTL = TimeUnit.MINUTES.toMillis(10);

    @Config("orbit.actors.deactivationTimeoutMillis")
    private long deactivationTimeoutMillis = TimeUnit.MINUTES.toMillis(2);

    private Task<Void> startPromise = new Task<>();

    public enum StageMode
    {
        CLIENT, // no activations
        HOST // allows activations
    }

    private ClusterPeer clusterPeer;
    private Messaging messaging;
    private Execution execution;
    private Hosting hosting;
    private boolean startCalled;
    private Clock clock;
    private ExecutorService executionPool;
    private ExecutionObjectCloner objectCloner;
    private MessageSerializer messageSerializer;
    private final WeakReference<ActorRuntime> cachedRef = new WeakReference<>(this);

    static
    {
        // this is here to help people testing the orbit source code.
        // because Async.init is removed by the build time bytecode instrumentation.
        Async.init();
    }

    public static class Builder
    {

        private Clock clock;
        private ExecutorService executionPool;
        private ExecutionObjectCloner objectCloner;
        private ClusterPeer clusterPeer;

        private String clusterName;
        private String nodeName;
        private StageMode mode = StageMode.HOST;
        private int executionPoolSize = DEFAULT_EXECUTION_POOL_SIZE;

        private Messaging messaging;

        private List<ActorExtension> extensions = new ArrayList<>();
        private Set<String> stickyHeaders = new HashSet<>();

        private Timer timer;

        public Builder clock(Clock clock)
        {
            this.clock = clock;
            return this;
        }

        public Builder executionPool(ExecutorService executionPool)
        {
            this.executionPool = executionPool;
            return this;
        }

        public Builder clusterPeer(ClusterPeer clusterPeer)
        {
            this.clusterPeer = clusterPeer;
            return this;
        }

        public Builder objectCloner(ExecutionObjectCloner objectCloner)
        {
            this.objectCloner = objectCloner;
            return this;
        }

        public Builder clusterName(String clusterName)
        {
            this.clusterName = clusterName;
            return this;
        }

        public Builder nodeName(String nodeName)
        {
            this.nodeName = nodeName;
            return this;
        }

        public Builder mode(StageMode mode)
        {
            this.mode = mode;
            return this;
        }

        public Builder messaging(Messaging messaging)
        {
            this.messaging = messaging;
            return this;
        }

        public Builder extensions(ActorExtension... extensions)
        {
            Collections.addAll(this.extensions, extensions);
            return this;
        }

        public Builder stickyHeaders(String... stickyHeaders)
        {
            Collections.addAll(this.stickyHeaders, stickyHeaders);
            return this;
        }

        public Builder timer(Timer timer)
        {
            this.timer = timer;
            return this;
        }

        public Stage build()
        {
            Stage stage = new Stage();
            stage.setClock(clock);
            stage.setExecutionPool(executionPool);
            stage.setObjectCloner(objectCloner);
            stage.setClusterName(clusterName);
            stage.setClusterPeer(clusterPeer);
            stage.setNodeName(nodeName);
            stage.setMode(mode);
            stage.setExecutionPoolSize(executionPoolSize);
            stage.setTimer(timer);
            extensions.forEach(stage::addExtension);
            stage.setMessaging(messaging);
            stage.addStickyHeaders(stickyHeaders);
            return stage;
        }

    }

    public Stage()
    {
        ActorRuntime.setRuntime(cachedRef);
    }

    public void addStickyHeaders(Collection<String> stickyHeaders)
    {
        this.stickyHeaders.addAll(stickyHeaders);
    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public void setMessaging(final Messaging messaging)
    {
        this.messaging = messaging;
    }

    public void setExecutionPool(final ExecutorService executionPool)
    {
        this.executionPool = executionPool;
    }

    public ExecutorService getExecutionPool()
    {
        return executionPool;
    }

    public int getExecutionPoolSize()
    {
        return executionPoolSize;
    }

    public void setExecutionPoolSize(int defaultPoolSize)
    {
        this.executionPoolSize = defaultPoolSize;
    }

    public ExecutionObjectCloner getObjectCloner()
    {
        return objectCloner;
    }

    public void setObjectCloner(ExecutionObjectCloner objectCloner)
    {
        this.objectCloner = objectCloner;
    }

    @SuppressWarnings("unused")
    public long getLocalObjectCount() {
        return objects.getLocalObjectCount();
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
            throw new IllegalStateException("Stage mode cannot be changed after startup. " + this.toString());
        }
        this.mode = mode;
    }

    public void setTimer(final Timer timer)
    {
        this.timer = timer;
    }

    public Task<Void> getStartPromise()
    {
        return startPromise;
    }

    public Task<?> start()
    {
        extensions = new ArrayList<>(extensions);
        startCalled = true;
        if (state != null)
        {
            throw new IllegalStateException("Can't start the stage at this state. " + this.toString());
        }
        state = NodeCapabilities.NodeState.RUNNING;

        if(timer == null)
        {
            timer = new Timer("orbit-stage-timer");
        }

        if (loggerExtension == null)
        {
            loggerExtension = getFirstExtension(LoggerExtension.class);
            if (loggerExtension == null)
            {
                loggerExtension = new DefaultLoggerExtension();
            }
        }
        logger = loggerExtension.getLogger(this);

        if (clusterName == null || clusterName.isEmpty())
        {
            setClusterName("orbit-cluster");
        }

        if (nodeName == null || nodeName.isEmpty())
        {
            setNodeName(getClusterName());
        }

        if (executionPool == null)
        {
            final ExecutorService newService = ExecutorUtils.newScalingThreadPool(executionPoolSize);
            executionPool = newService;
        }

        executionSerializer = new WaitFreeMultiExecutionSerializer<>(executionPool);

        if (hosting == null)
        {
            hosting =  new Hosting();
        }
        if (messaging == null)
        {
            messaging =  new Messaging();
        }
        if (execution == null)
        {
            execution = new Execution();
        }
        if (messageSerializer == null)
        {
            messageSerializer = new JavaMessageSerializer();
        }
        if (clusterPeer == null)
        {
            clusterPeer = new JGroupsClusterPeer();
        }
        if (clock == null)
        {
            clock = Clock.systemUTC();
        }
        if (objectCloner == null)
        {
            objectCloner = new KryoCloner();
        }

        finder = getFirstExtension(ActorClassFinder.class);
        if (finder == null)
        {
            finder = new DefaultActorClassFinder();
            finder.start().join();
        }

        cacheManager = new ResponseCaching();

        hosting.setNodeType(mode == StageMode.HOST ? NodeCapabilities.NodeTypeEnum.SERVER : NodeCapabilities.NodeTypeEnum.CLIENT);
        execution.setRuntime(this);
        execution.setObjects(objects);
        execution.setExecutionSerializer(executionSerializer);

        cacheManager.setObjectCloner(objectCloner);
        cacheManager.setRuntime(this);
        cacheManager.setMessageSerializer(messageSerializer);

        messaging.setRuntime(this);

        hosting.setStage(this);
        hosting.setClusterPeer(clusterPeer);
        pipeline = new DefaultPipeline();

        // caches responses
        pipeline.addLast(DefaultHandlers.CACHING, cacheManager);

        pipeline.addLast(DefaultHandlers.EXECUTION, execution);

        // handles invocation messages and request-response matching
        pipeline.addLast(DefaultHandlers.HOSTING, hosting);


        // handles invocation messages and request-response matching
        pipeline.addLast(DefaultHandlers.MESSAGING, messaging);

        final MessageLoopback messageLoopback = new MessageLoopback();
        messageLoopback.setCloner(objectCloner instanceof NoOpCloner ? new KryoCloner() : objectCloner);
        messageLoopback.setRuntime(this);
        pipeline.addLast(messageLoopback.getName(), messageLoopback);

        // message serializer handler
        pipeline.addLast(DefaultHandlers.SERIALIZATION, new SerializationHandler(this, messageSerializer));

        // cluster peer handler
        pipeline.addLast(DefaultHandlers.NETWORK, new ClusterHandler(clusterPeer, clusterName, nodeName));

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


        StreamProvider defaultStreamProvider = extensions.stream()
                .filter(p -> p instanceof StreamProvider)
                .map(p -> (StreamProvider) p)
                .filter(p -> StringUtils.equals(p.getName(), AsyncStream.DEFAULT_PROVIDER)).findFirst().orElse(null);

        if (defaultStreamProvider == null)
        {
            defaultStreamProvider = new SimpleStreamExtension(AsyncStream.DEFAULT_PROVIDER);
            extensions.add(defaultStreamProvider);
        }

        messaging.start();
        hosting.start();
        execution.start();

        await(Task.allOf(extensions.stream().map(Startable::start)));

        Task<Void> future = pipeline.connect(null);
        if (mode == StageMode.HOST)
        {
            future = future.thenRun(() -> {
                this.bind();
                Actor.getReference(ReminderController.class).ensureStart();
            });
        }

        future = future.thenRun(() -> bind());

        // schedules the pulse
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (state == NodeCapabilities.NodeState.RUNNING)
                {
                    ForkJoinTask.adapt(() -> pulse().join()).fork();
                }
            }
        }, pulseIntervalMillis, pulseIntervalMillis);

        future.whenComplete((r, e) -> {
            if (e != null)
            {
                startPromise.completeExceptionally(e);
            }
            else
            {
                startPromise.complete(r);
            }
        });
        await(startPromise);
        if (logger.isDebugEnabled())
        {
            logger.debug("Stage started [{}]", runtimeIdentity());
        }
        return Task.done();
    }

    public void setClusterPeer(final ClusterPeer clusterPeer)
    {
        this.clusterPeer = clusterPeer;
    }

    /**
     * Installs extensions to the stage.
     * <p>
     * Example:
     * <pre>
     * stage.addExtension(new MongoDbProvider(...));
     * </pre>
     *
     * @param extension Actor Extensions instance.
     */
    public void addExtension(final ActorExtension extension)
    {
        this.extensions.add(extension);
    }


    public Task<?> stop()
    {
        if (getState() != NodeCapabilities.NodeState.RUNNING)
        {
            throw new IllegalStateException("Stage node state is not running, state: " + getState());
        }

        state = NodeCapabilities.NodeState.STOPPING;

        // * refuse new actor activations
        // first notify other nodes

        // * deactivate all actors
        // * notify rest of the cluster (no more observer messages)
        // * finalize all timers
        // * stop processing new received messages
        // * wait pending tasks execution
        // * stop the network

        logger.debug("start stopping pipeline");
        await(pipeline.write(NodeCapabilities.NodeState.STOPPING));

        logger.debug("stopping actors");
        await(stopActors());
        logger.debug("stopping timers");
        await(stopTimers());
        do
        {
            InternalUtils.sleep(100);
        } while (executionSerializer.isBusy());
        logger.debug("closing pipeline");
        await(pipeline.close());

        logger.debug("stopping execution serializer");
        executionSerializer.shutdown();

        logger.debug("stopping extensions");
        await(stopExtensions());

        state = NodeCapabilities.NodeState.STOPPED;
        logger.debug("stop done");

        return Task.done();
    }

    private Task<Void> stopActors()
    {
        for (int passes = 0; passes < 2; passes++)
        {
            // using negative age meaning all actors, regardless of age
            cleanupActors(Long.MIN_VALUE);
        }
        return Task.done();
    }

    @Deprecated
    public Task<Void> cleanupActors()
    {
        return cleanupActors(defaultActorTTL);
    }


    private Task<Void> cleanupObservers()
    {
        objects.stream()
                .filter(e -> e.getValue() instanceof ObserverEntry && e.getValue().getObject() == null)
                .forEach(e -> objects.remove(e.getKey(), e.getValue()));
        return Task.done();
    }

    private Task<Void> cleanupActors(long maxAge)
    {
        // avoid sorting since lastAccess changes
        // and O(N) is still smaller than O(N Log N)
        final Iterator<Map.Entry<Object, LocalObjects.LocalObjectEntry>> iterator = objects.stream()
                .filter(e -> e.getValue() instanceof ActorBaseEntry)
                .iterator();

        final List<Task<Void>> pending = new ArrayList<>();

        // ensure that certain number of concurrent deactivations is happening at each moment
        while (iterator.hasNext())
        {
            while (pending.size() < concurrentDeactivations && iterator.hasNext())
            {

                final Map.Entry<Object, LocalObjects.LocalObjectEntry> entryEntry = iterator.next();
                final ActorBaseEntry<?> actorEntry = (ActorBaseEntry<?>) entryEntry.getValue();
                if (actorEntry.isDeactivated())
                {
                    // this might happen if the deactivation is called outside this loop,
                    // for instance by the stateless worker that owns the objects
                    objects.remove(entryEntry.getKey(), entryEntry.getValue());
                }

                // Skip this actor if it is marked NeverDeactivate
                if (RemoteReference.getInterfaceClass(actorEntry.getRemoteReference()).isAnnotationPresent(NeverDeactivate.class)) {
                    continue;
                }

                if (clock().millis() - actorEntry.getLastAccess() > maxAge)
                {
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("deactivating {}", actorEntry.getRemoteReference());
                    }
                    pending.add(actorEntry.deactivate().failAfter(deactivationTimeoutMillis, TimeUnit.MILLISECONDS)
                            .whenComplete((r, e) -> {
                                // ensures removal
                                if (e != null)
                                {
                                    // error occurred
                                    if (logger.isErrorEnabled())
                                    {
                                        logger.error("Error during the deactivation of " + actorEntry.getRemoteReference(), e);
                                    }
                                    // forcefully setting the entry to deactivated
                                    actorEntry.setDeactivated(true);
                                }
                                objects.remove(entryEntry.getKey(), entryEntry.getValue());
                                if (entryEntry.getKey() == actorEntry.getRemoteReference())
                                {
                                    // removing non stateless actor from the distributed directory
                                    getHosting().actorDeactivated(actorEntry.getRemoteReference());
                                }
                            }));
                }
            }
            if (pending.size() > 0)
            {
                // await for at least one deactivation to complete
                await(Task.anyOf(pending));
                // remove all completed deactivations
                for (int i = pending.size(); --i >= 0; )
                {
                    if (pending.get(i).isDone())
                    {
                        pending.remove(i);
                    }
                }
            }
        }
        if (pending.size() > 0)
        {
            await(Task.allOf(pending));
        }
        return Task.done();
    }

    private Task<Void> stopTimers()
    {
        try
        {
            timer.cancel();
        }
        catch (Throwable ex)
        {
            logger.error("Error stopping timers", ex);
        }
        return Task.done();
    }

    private Task<Void> stopExtensions()
    {
        for (ActorExtension e : getExtensions())
        {
            try
            {
                await(e.stop());
            }
            catch (Throwable ex)
            {
                logger.error("Error stopping extension: " + e);
            }
        }
        return Task.done();
    }

    public Hosting getHosting()
    {
        return hosting;
    }

    public ClusterPeer getClusterPeer()
    {
        return clusterPeer != null ? clusterPeer : (clusterPeer = new JGroupsClusterPeer());
    }

    public Task pulse()
    {
        Actor.getReference(ReminderController.class).ensureStart();
        return cleanup();
    }


    public Task cleanup()
    {
        await(execution.cleanup());
        await(cleanupActors(defaultActorTTL));
        await(cleanupObservers());
        await(messaging.cleanup());

        return Task.done();
    }

    /**
     * Binds this stage to the current thread.
     * This tells ungrounded references to use this stage to call remote methods.
     * <p>
     * An ungrounded reference is a reference created with {@code Actor.getRemoteReference} and used outside of an actor method.
     * <p>
     * This is only necessary when there are <i>two or more</i> OrbitStages active in the same virtual machine and
     * remote calls need to be issued from outside an actor.
     * This method was created to help with test cases.
     * <p>
     * A normal application will have a single stage and should have no reason to call this method.
     * <p>
     * This method writes a weak reference to the runtime in a thread local.
     * No cleanup is necessary, so none is available.
     */
    public void bind()
    {
        ActorRuntime.setRuntime(this.cachedRef);
    }

    @Override
    public List<NodeAddress> getAllNodes()
    {
        if (hosting == null)
        {
            return Collections.emptyList();
        }
        return hosting.getAllNodes();
    }

    public List<NodeAddress> getServerNodes()
    {
        if (hosting == null)
        {
            return Collections.emptyList();
        }
        return hosting.getServerNodes();
    }

    public NodeCapabilities.NodeState getState()
    {
        return state;
    }

    public ActorRuntime getRuntime()
    {
        return this;
    }

    public MessageSerializer getMessageSerializer()
    {
        return messageSerializer;
    }

    public void setMessageSerializer(final MessageSerializer messageSerializer)
    {
        this.messageSerializer = messageSerializer;
    }

    @Override
    public Task<?> invoke(final RemoteReference toReference, final Method m, final boolean oneWay, final int methodId, final Object[] params)
    {
        if (state == NodeCapabilities.NodeState.STOPPED)
        {
            throw new IllegalStateException("Stage is stopped. " + this.toString());
        }
        final Invocation invocation = new Invocation(toReference, m, oneWay, methodId, params, null);
        // copy stick context valued to the message headers headers
        final ActorTaskContext context = ActorTaskContext.current();
        if (context != null)
        {
            LinkedHashMap<Object, Object> headers = null;
            for (String key : stickyHeaders)
            {
                final Object value = context.getProperty(key);
                if (value != null)
                {
                    if (headers == null)
                    {
                        headers = new LinkedHashMap<>();
                    }
                    headers.put(key, value);
                }
            }
            invocation.setHeaders(headers);
        }

        final Task<Void> result = pipeline.write(invocation);
        return result;
    }

    @Override
    public Registration registerTimer(final AbstractActor<?> actor,
                                      final Callable<Task<?>> taskCallable,
                                      final long dueTime, final long period,
                                      final TimeUnit timeUnit)
    {
        final Object key = actor.getClass().isAnnotationPresent(StatelessWorker.class)
                ? actor : RemoteReference.from(actor);

        final ActorEntry localActor = (ActorEntry) objects.findLocalActor((Actor) actor);

        if (localActor == null || localActor.isDeactivated())
        {
            throw new IllegalStateException("Actor is deactivated");
        }

        class MyRegistration implements Registration
        {
            TimerTask task;

            @Override
            public void dispose()
            {
                if (task != null)
                {
                    task.cancel();
                }
                task = null;
            }
        }

        final TimerTask timerTask = new TimerTask()
        {
            boolean canceled;

            @Override
            public void run()
            {
                if (localActor.isDeactivated())
                {
                    cancel();
                    return;
                }

                executionSerializer.offerJob(key,
                        () -> {
                            if (localActor.isDeactivated())
                            {
                                cancel();
                            }
                            else
                            {
                                try
                                {
                                    if (!canceled)
                                    {
                                        return (Task) taskCallable.call();
                                    }
                                }
                                catch (Exception ex)
                                {
                                    logger.warn("Error calling timer", ex);
                                }
                            }
                            return (Task) Task.done();
                        }, 1000);
            }

            @Override
            public boolean cancel()
            {
                canceled = true;
                return super.cancel();
            }
        };

        MyRegistration registration = new MyRegistration();
        registration.task = timerTask;

        // this ensures that the timers get removed during deactivation
        localActor.addTimer(registration);

        if (period > 0)
        {
            timer.schedule(timerTask, timeUnit.toMillis(dueTime), timeUnit.toMillis(period));
        }
        else
        {
            timer.schedule(timerTask, timeUnit.toMillis(dueTime));
        }

        return registration;
    }


    @Override
    public Clock clock()
    {
        return clock;
    }

    @Override
    public Task<?> registerReminder(final Remindable actor, final String reminderName, final long dueTime, final long period, final TimeUnit timeUnit)
    {
        return Actor.getReference(ReminderController.class).registerOrUpdateReminder(actor, reminderName, new Date(clock.millis() + timeUnit.toMillis(dueTime)), period, timeUnit);
    }

    @Override
    public Task<?> unregisterReminder(final Remindable actor, final String reminderName)
    {
        return Actor.getReference(ReminderController.class).unregisterReminder(actor, reminderName);
    }

    @Override
    public String runtimeIdentity()
    {
        return runtimeIdentity;
    }

    @Override
    public Task<NodeAddress> locateActor(final Addressable actorReference, final boolean forceActivation)
    {
        return hosting.locateActor((RemoteReference<?>) actorReference, forceActivation);
    }

    @Override
    public NodeAddress getLocalAddress()
    {
        return hosting.getNodeAddress();
    }

    @Override
    public <T extends ActorObserver> T registerObserver(Class<T> iClass, String id, final T observer)
    {
        final RemoteReference<T> reference = objects.getOrAddLocalObjectReference(hosting.getNodeAddress(), iClass, id, observer);
        RemoteReference.setRuntime(reference, this);
        //noinspection unchecked
        return iClass != null ? iClass.cast(reference) : (T) reference;
    }

    public <T> T getReference(BasicRuntime runtime, NodeAddress address, Class<T> iClass, Object id)
    {
        return DefaultDescriptorFactory.get().getReference(this, address, iClass, id);
    }

    @Override
    public StreamProvider getStreamProvider(final String providerName)
    {
        StreamProvider streamProvider = getAllExtensions(StreamProvider.class).stream()
                .filter(p -> StringUtils.equals(p.getName(), providerName))
                .findFirst().orElseThrow(() -> new UncheckedException(String.format("Provider: %s not found", providerName)));

        final AbstractActor<?> actor = ActorTaskContext.currentActor();
        if (actor != null)
        {
            @SuppressWarnings("unchecked")
            ActorEntry<AbstractActor> actorEntry = (ActorEntry<AbstractActor>) objects.findLocalActor((Actor) actor);

            // wraps the stream provider to ensure sequential execution
            return new StreamProvider()
            {
                @Override
                public <T> AsyncStream<T> getStream(final Class<T> dataClass, final String id)
                {
                    final AsyncStream<T> stream = streamProvider.getStream(dataClass, id);
                    return new AsyncStreamReference<>(providerName, dataClass, id, new AsyncStream<T>()
                    {
                        @Override
                        public Task<Void> unsubscribe(final StreamSubscriptionHandle<T> handle)
                        {
                            // removes the subscription reminder from the actor entry.
                            actorEntry.removeStreamSubscription(handle, stream);
                            return stream.unsubscribe(handle);
                        }

                        @Override
                        public Task<StreamSubscriptionHandle<T>> subscribe(final AsyncObserver<T> observer, StreamSequenceToken sequenceToken)
                        {

                            Task<StreamSubscriptionHandle<T>> subscriptionTask = stream.subscribe(new AsyncObserver<T>()
                            {
                                @Override
                                public Task<Void> onNext(final T data, final StreamSequenceToken sequenceToken)
                                {
                                    // runs with the actor execution serialization concerns
                                    return actorEntry.run(entry -> observer.onNext(data, null));
                                }

                                @Override
                                public Task<Void> onError(final Exception ex)
                                {
                                    // runs with the actor execution serialization concerns
                                    return actorEntry.run(entry -> observer.onError(ex));
                                }
                            }, sequenceToken);

                            // this allows the actor to unsubscribe automatically on deactivation
                            actorEntry.addStreamSubscription(await(subscriptionTask), stream);
                            return subscriptionTask;
                        }

                        @Override
                        public Task<Void> publish(final T data)
                        {
                            return stream.publish(data);
                        }
                    });
                }

                @Override
                public String getName()
                {
                    return streamProvider.getName();
                }
            };
        }
        return streamProvider;
    }


    @Override
    public <T> AsyncStream<T> getStream(final String provider, final Class<T> dataClass, final String id)
    {
        return getStreamProvider(provider).getStream(dataClass, id);
    }

    @Override
    public List<ActorExtension> getExtensions()
    {
        return extensions;
    }

    <T> LocalObjects.LocalObjectEntry createLocalObjectEntry(final RemoteReference<T> reference, final T object)
    {
        final Class<T> interfaceClass = RemoteReference.getInterfaceClass(reference);
        if (Actor.class.isAssignableFrom(interfaceClass))
        {
            final ActorBaseEntry actorEntry;
            if (interfaceClass.isAnnotationPresent(StatelessWorker.class))
            {
                actorEntry = new StatelessActorEntry<>(objects, reference);
            }
            else
            {
                actorEntry = new ActorEntry<>(reference);
            }
            actorEntry.setExecutionSerializer(executionSerializer);
            actorEntry.setLoggerExtension(loggerExtension);
            actorEntry.setRuntime(this);
            final Class actorImplementation = finder.findActorImplementation((Class) interfaceClass);
            actorEntry.setConcreteClass(actorImplementation);
            actorEntry.setStorageExtension(getStorageExtensionFor(actorImplementation));
            return actorEntry;
        }
        if (ActorObserver.class.isAssignableFrom(interfaceClass))
        {
            final ObserverEntry observerEntry = new ObserverEntry(reference, object);
            observerEntry.setExecutionSerializer(executionSerializer);
            return observerEntry;
        }
        throw new IllegalArgumentException("Invalid object type: " + object.getClass());
    }

    @SuppressWarnings("unchecked")
    public <T extends ActorExtension> T getStorageExtensionFor(Class actorClass)
    {
        if (extensions == null)
        {
            return null;
        }
        final Annotation annotation = actorClass.getAnnotation(StorageExtension.class);
        StorageExtension ann = (StorageExtension) annotation;
        String extensionName = ann == null ? "default" : ann.value();
        // selects the fist provider with the right name
        return (T) extensions.stream()
                .filter(p -> (p instanceof cloud.orbit.actors.extensions.StorageExtension) && extensionName.equals(((cloud.orbit.actors.extensions.StorageExtension) p).getName()))
                .findFirst()
                .orElse(null);
    }


    public boolean canActivateActor(final String interfaceName)
    {
        if (getState() != NodeCapabilities.NodeState.RUNNING)
        {
            // todo, improve this
            if (hosting.getServerNodes().size() > 1)
            {
                return false;
            }
        }
        Class<Actor> aInterface = InternalUtils.classForName(interfaceName, true);
        if (aInterface == null)
        {
            return false;
        }
        final Class<?> concreteClass = finder.findActorImplementation(aInterface);
        return concreteClass != null;
    }

    public Pipeline getPipeline()
    {
        return pipeline;
    }

    @Override
    public Logger getLogger(Object object)
    {
        return loggerExtension.getLogger(object);
    }

    public Set<String> getStickyHeaders()
    {
        return stickyHeaders;
    }

    @Override
    public String toString()
    {
        return "Stage{" +
                "state=" + state +
                ", runtimeIdentity='" + runtimeIdentity + '\'' +
                '}';
    }
}
