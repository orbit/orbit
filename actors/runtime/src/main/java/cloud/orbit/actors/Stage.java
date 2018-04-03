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

import com.ea.async.Async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.annotation.StatelessWorker;
import cloud.orbit.actors.annotation.StorageExtension;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.actors.cluster.ClusterPeer;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.concurrent.MultiExecutionSerializer;
import cloud.orbit.actors.concurrent.WaitFreeMultiExecutionSerializer;
import cloud.orbit.actors.extensions.ActorClassFinder;
import cloud.orbit.actors.extensions.ActorConstructionExtension;
import cloud.orbit.actors.extensions.ActorDeactivationExtension;
import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.actors.extensions.DefaultLoggerExtension;
import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.extensions.LoggerExtension;
import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.extensions.NodeSelectorExtension;
import cloud.orbit.actors.extensions.PipelineExtension;
import cloud.orbit.actors.extensions.ResponseCachingExtension;
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
import cloud.orbit.actors.runtime.DefaultActorConstructionExtension;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.DefaultInvocationHandler;
import cloud.orbit.actors.runtime.DefaultLifetimeExtension;
import cloud.orbit.actors.runtime.DefaultLocalObjectsCleaner;
import cloud.orbit.actors.runtime.Execution;
import cloud.orbit.actors.runtime.FastActorClassFinder;
import cloud.orbit.actors.runtime.Hosting;
import cloud.orbit.actors.runtime.InternalUtils;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.InvocationHandler;
import cloud.orbit.actors.runtime.KryoSerializer;
import cloud.orbit.actors.runtime.LazyActorClassFinder;
import cloud.orbit.actors.runtime.LocalObjects;
import cloud.orbit.actors.runtime.LocalObjectsCleaner;
import cloud.orbit.actors.runtime.MessageLoopback;
import cloud.orbit.actors.runtime.Messaging;
import cloud.orbit.actors.runtime.NodeCapabilities;
import cloud.orbit.actors.runtime.ObserverEntry;
import cloud.orbit.actors.runtime.RandomSelectorExtension;
import cloud.orbit.actors.runtime.Registration;
import cloud.orbit.actors.runtime.ReminderController;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.actors.runtime.DefaultResponseCachingExtension;
import cloud.orbit.actors.runtime.RuntimeActions;
import cloud.orbit.actors.runtime.SerializationHandler;
import cloud.orbit.actors.runtime.ShardedReminderController;
import cloud.orbit.actors.runtime.StatelessActorEntry;
import cloud.orbit.actors.streams.AsyncObserver;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSequenceToken;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.actors.streams.simple.SimpleStreamExtension;
import cloud.orbit.actors.util.IdUtils;
import cloud.orbit.annotation.Config;
import cloud.orbit.concurrent.ExecutorUtils;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import cloud.orbit.lifecycle.Startable;
import cloud.orbit.util.StringUtils;

import javax.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.ea.async.Async.await;

@Singleton
public class Stage implements Startable, ActorRuntime, RuntimeActions
{
    private Logger logger = LoggerFactory.getLogger(Stage.class);

    private static final int DEFAULT_EXECUTION_POOL_SIZE = 32;
    private static final int DEFAULT_LOCAL_ADDRESS_CACHE_MAXIMUM_SIZE = 10_000;

    private final String runtimeIdentity = "Orbit[" + IdUtils.urlSafeString(128) + "]";

    private final WeakReference<ActorRuntime> cachedRef = new WeakReference<>(this);

    private final LocalObjects objects = new LocalObjects()
    {
        @Override
        protected <T> LocalObjectEntry createLocalObjectEntry(final RemoteReference<T> reference, final T object)
        {
            return Stage.this.createLocalObjectEntry(reference, object);
        }
    };

    private final Executor shutdownExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("OrbitShutdownThread");
        thread.setDaemon(true);
        return thread;
    });

    @Config("orbit.actors.clusterName")
    private String clusterName;

    @Config("orbit.actors.nodeName")
    private String nodeName;

    @Config("orbit.actors.stageMode")
    private StageMode mode = StageMode.HOST;

    @Config("orbit.actors.executionPoolSize")
    private int executionPoolSize = DEFAULT_EXECUTION_POOL_SIZE;

    @Config("orbit.actors.localAddressCacheMaximumSize")
    private int localAddressCacheMaximumSize = DEFAULT_LOCAL_ADDRESS_CACHE_MAXIMUM_SIZE;

    @Config("orbit.actors.extensions")
    private List<ActorExtension> extensions = new CopyOnWriteArrayList<>();

    @Config("orbit.actors.stickyHeaders")
    private Set<String> stickyHeaders = new HashSet<>();

    @Config("orbit.actors.basePackages")
    private List<String> basePackages = new ArrayList<>();

    @Config("orbit.actors.pulseInterval")
    private long pulseIntervalMillis = TimeUnit.SECONDS.toMillis(10);

    @Config("orbit.actors.concurrentDeactivations")
    private int concurrentDeactivations = 16;

    @Config("orbit.actors.defaultActorTTL")
    private long defaultActorTTL = TimeUnit.MINUTES.toMillis(10);

    @Config("orbit.actors.deactivationTimeoutMillis")
    private long deactivationTimeoutMillis = TimeUnit.SECONDS.toMillis(10);

    @Config("orbit.actors.localAddressCacheTTL")
    private long localAddressCacheTTL = defaultActorTTL + deactivationTimeoutMillis;

    @Config("orbit.actors.numReminderControllers")
    private int numReminderControllers = 1;

    @Config("orbit.actors.broadcastActorDeactivations")
    private boolean broadcastActorDeactivations = true;

    private boolean enableShutdownHook = true;

    private boolean enableMessageLoopback = true;

    private volatile NodeCapabilities.NodeState state;

    private ClusterPeer clusterPeer;
    private Messaging messaging;
    private InvocationHandler invocationHandler;
    private Execution execution;
    private Hosting hosting;
    private boolean startCalled;
    private Clock clock;
    private ExecutorService executionPool;
    private ExecutionObjectCloner objectCloner;
    private ExecutionObjectCloner messageLoopbackObjectCloner;
    private MessageSerializer messageSerializer;
    private LocalObjectsCleaner localObjectsCleaner;

    private MultiExecutionSerializer<Object> executionSerializer;
    private ActorClassFinder finder;
    private LoggerExtension loggerExtension;

    private Timer timer;
    private Pipeline pipeline;

    private final Task<Void> startPromise = new Task<>();
    private Thread shutdownHook = null;
    private final Object shutdownLock = new Object();

    public enum StageMode
    {
        CLIENT, // no activations
        HOST // allows activations
    }

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
        private ExecutionObjectCloner messageLoopbackObjectCloner;
        private MessageSerializer messageSerializer;
        private ClusterPeer clusterPeer;
        private Messaging messaging;
        private InvocationHandler invocationHandler;
        private Execution execution;
        private LocalObjectsCleaner localObjectsCleaner;

        private String clusterName;
        private String nodeName;
        private StageMode mode = StageMode.HOST;
        private int executionPoolSize = DEFAULT_EXECUTION_POOL_SIZE;
        private int localAddressCacheMaximumSize = DEFAULT_LOCAL_ADDRESS_CACHE_MAXIMUM_SIZE;

        private List<ActorExtension> extensions = new ArrayList<>();
        private Set<String> stickyHeaders = new HashSet<>();
        private List<String> basePackages = new ArrayList<>();

        private Long actorTTLMillis = null;
        private Long localAddressCacheTTLMillis = null;
        private Integer numReminderControllers = null;
        private Boolean broadcastActorDeactivations = null;
        private Long deactivationTimeoutMillis;
        private Integer concurrentDeactivations;
        private Boolean enableShutdownHook = null;
        private Boolean enableMessageLoopback;

        private Timer timer;

        public Builder clock(Clock clock)
        {
            this.clock = clock;
            return this;
        }

        public Builder enableMessageLoopback(Boolean enableMessageLoopback)
        {
            this.enableMessageLoopback = enableMessageLoopback;
            return this;
        }

        public Builder executionPool(ExecutorService executionPool)
        {
            this.executionPool = executionPool;
            return this;
        }

        public Builder executionPoolSize(int executionPoolSize)
        {
            this.executionPoolSize = executionPoolSize;
            return this;
        }

        public Builder localAddressCacheMaximumSize(int localAddressCacheMaximumSize)
        {
            this.localAddressCacheMaximumSize = localAddressCacheMaximumSize;
            return this;
        }

        public Builder execution(Execution execution)
        {
            this.execution = execution;
            return this;
        }

        public Builder localObjectsCleaner(LocalObjectsCleaner localObjectsCleaner)
        {
            this.localObjectsCleaner = localObjectsCleaner;
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

        public Builder messageSerializer(MessageSerializer messageSerializer)
        {
            this.messageSerializer = messageSerializer;
            return this;
        }

        public Builder messageLoopbackObjectCloner(ExecutionObjectCloner messageLoopbackObjectCloner)
        {
            this.messageLoopbackObjectCloner = messageLoopbackObjectCloner;
            return this;
        }

        public Builder messaging(Messaging messaging)
        {
            this.messaging = messaging;
            return this;
        }

        public Builder invocationHandler(InvocationHandler invocationHandler)
        {
            this.invocationHandler = invocationHandler;
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

        public Builder extensions(Collection<ActorExtension> extensions)
        {
            this.extensions.addAll(extensions);
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

        public Builder basePackages(String... basePackages)
        {
            Collections.addAll(this.basePackages, basePackages);
            return this;
        }

        public Builder basePackages(Collection<String> basePackages)
        {
            this.basePackages.addAll(basePackages);
            return this;
        }

        public Builder timer(Timer timer)
        {
            this.timer = timer;
            return this;
        }

        public Builder actorTTL(final long duration, final TimeUnit timeUnit)
        {
            this.actorTTLMillis = timeUnit.toMillis(duration);
            return this;
        }

        public Builder localAddressCacheTTL(final long duration, final TimeUnit timeUnit) {
            this.localAddressCacheTTLMillis = timeUnit.toMillis(duration);
            return this;
        }

        public Builder numReminderControllers(final int numReminderControllers)
        {
            if(numReminderControllers < 1)
            {
                throw new IllegalArgumentException("Must specify at least 1 reminder controller");
            }
            this.numReminderControllers = numReminderControllers;
            return this;
        }

        public Builder deactivationTimeout(final long duration, final TimeUnit timeUnit)
        {
            this.deactivationTimeoutMillis = timeUnit.toMillis(duration);
            return this;
        }

        public Builder concurrentDeactivations(final int concurrentDeactivations)
        {
            this.concurrentDeactivations = concurrentDeactivations;
            return this;
        }

        public Builder broadcastActorDeactivations(final boolean broadcastActorDeactivations)
        {
            this.broadcastActorDeactivations = broadcastActorDeactivations;
            return this;
        }

        public Builder enableShutdownHook(final boolean enableShutdownHook) {
            this.enableShutdownHook = enableShutdownHook;
            return this;
        }

        public Stage build()
        {
            final Stage stage = new Stage();
            stage.setClock(clock);
            stage.setExecutionPool(executionPool);
            stage.setExecution(execution);
            stage.setObjectCloner(objectCloner);
            stage.setMessageLoopbackObjectCloner(messageLoopbackObjectCloner);
            stage.setMessageSerializer(messageSerializer);
            stage.setClusterName(clusterName);
            stage.setClusterPeer(clusterPeer);
            stage.setNodeName(nodeName);
            stage.setMode(mode);
            stage.setExecutionPoolSize(executionPoolSize);
            stage.setLocalAddressCacheMaximumSize(localAddressCacheMaximumSize);
            stage.setLocalObjectsCleaner(localObjectsCleaner);
            stage.setTimer(timer);
            extensions.forEach(stage::addExtension);
            stage.setInvocationHandler(invocationHandler);
            stage.setMessaging(messaging);
            stage.addStickyHeaders(stickyHeaders);
            stage.addBasePackages(basePackages);
            if(actorTTLMillis != null) stage.setDefaultActorTTL(actorTTLMillis);
            if(localAddressCacheTTLMillis != null) stage.setLocalAddressCacheTTL(localAddressCacheTTLMillis);
            if(numReminderControllers != null) stage.setNumReminderControllers(numReminderControllers);
            if(deactivationTimeoutMillis != null) stage.setDeactivationTimeout(deactivationTimeoutMillis);
            if(concurrentDeactivations != null) stage.setConcurrentDeactivations(concurrentDeactivations);
            if(broadcastActorDeactivations != null) stage.setBroadcastActorDeactivations(broadcastActorDeactivations);
            if(enableShutdownHook != null) stage.setEnableShutdownHook(enableShutdownHook);
            if(enableMessageLoopback != null) stage.setEnableMessageLoopback(enableMessageLoopback);
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

    public void addBasePackages(final List<String> basePackages)
    {
        this.basePackages.addAll(basePackages);
    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public void setMessaging(final Messaging messaging)
    {
        this.messaging = messaging;
    }

    public void setInvocationHandler(final InvocationHandler invocationHandler)
    {
        this.invocationHandler = invocationHandler;
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

    public void setLocalAddressCacheMaximumSize(final int localAddressCacheMaximumSize)
    {
        this.localAddressCacheMaximumSize = localAddressCacheMaximumSize;
    }

    public Execution getExecution()
    {
        return execution;
    }

    public void setExecution(Execution execution)
    {
        this.execution = execution;
    }

    public void setLocalObjectsCleaner(final LocalObjectsCleaner localObjectsCleaner)
    {
        this.localObjectsCleaner = localObjectsCleaner;
    }

    public LocalObjectsCleaner getLocalObjectsCleaner() {
        return localObjectsCleaner;
    }

    public ExecutionObjectCloner getObjectCloner()
    {
        return objectCloner;
    }

    public void setMessageLoopbackObjectCloner(final ExecutionObjectCloner messageLoopbackObjectCloner)
    {
        this.messageLoopbackObjectCloner = messageLoopbackObjectCloner;
    }

    public void setObjectCloner(final ExecutionObjectCloner objectCloner)
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

    public void setConcurrentDeactivations(int concurrentDeactivations)
    {
        this.concurrentDeactivations = concurrentDeactivations;
    }

    public void setDefaultActorTTL(long defaultActorTTLMs)
    {
        this.defaultActorTTL = defaultActorTTLMs;
    }

    public void setLocalAddressCacheTTL(final long localAddressCacheTTL) {
        this.localAddressCacheTTL = localAddressCacheTTL;
    }

    public void setNumReminderControllers(final int numReminderControllers)
    {
        if(numReminderControllers < 1)
        {
            throw new IllegalArgumentException("Must specify at least 1 reminder controller shard");
        }
        this.numReminderControllers = numReminderControllers;
    }

    public void setDeactivationTimeout(long deactivationTimeoutMs)
    {
        this.deactivationTimeoutMillis = deactivationTimeoutMs;
    }

    public boolean getBroadcastActorDeactivations()
    {
        return broadcastActorDeactivations;
    }

    public void setBroadcastActorDeactivations(boolean broadcastActorDeactivation)
    {
        this.broadcastActorDeactivations = broadcastActorDeactivation;
    }

    public void setEnableShutdownHook(boolean enableShutdownHook)
    {
        this.enableShutdownHook = enableShutdownHook;
    }

    public void setEnableMessageLoopback(final boolean enableMessageLoopback)
    {
        this.enableMessageLoopback = enableMessageLoopback;
    }

    @Override
    public Task<?> start()
    {
        logger.info("Starting Stage...");
        extensions = new ArrayList<>(extensions);
        startCalled = true;
        if (state != null)
        {
            throw new IllegalStateException("Can't start the stage at this state. " + this.toString());
        }
        state = NodeCapabilities.NodeState.RUNNING;

        if(timer == null)
        {
            timer = new Timer("OrbitTimer");
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
            executionPool = ExecutorUtils.newScalingThreadPool(executionPoolSize);
        }

        executionSerializer = new WaitFreeMultiExecutionSerializer<>(executionPool);

        if (hosting == null)
        {
            hosting = new Hosting(localAddressCacheMaximumSize, localAddressCacheTTL);
        }
        if (messaging == null)
        {
            messaging =  new Messaging();
        }
        if (execution == null)
        {
            execution = new Execution();
        }
        if(invocationHandler == null)
        {
            invocationHandler = new DefaultInvocationHandler();
        }
        if (messageSerializer == null)
        {
            messageSerializer = new KryoSerializer();
        }
        if (clusterPeer == null)
        {
            clusterPeer = constructDefaultClusterPeer();
        }
        if (clock == null)
        {
            clock = Clock.systemUTC();
        }
        if (objectCloner == null)
        {
            objectCloner = new KryoSerializer();
        }
        if (localObjectsCleaner == null)
        {
            localObjectsCleaner = new DefaultLocalObjectsCleaner(hosting, clock, executionPool, objects, defaultActorTTL, concurrentDeactivations, deactivationTimeoutMillis);
        }

        // create pipeline before waiting for ActorClassFinder as stop might be invoked before it is complete
        pipeline = new DefaultPipeline();

        finder = getFirstExtension(ActorClassFinder.class);
        if (finder == null)
        {
            if(!basePackages.isEmpty())
            {
                final String[] basePackagesArray = basePackages.toArray(new String[0]);
                finder = new FastActorClassFinder(basePackagesArray);
            }
            else
            {
                finder = new LazyActorClassFinder();
            }
        }
        await(finder.start());

        localObjectsCleaner.setActorDeactivationExtensions(getAllExtensions(ActorDeactivationExtension.class));

        final List<ResponseCachingExtension> cacheExtensions = getAllExtensions(ResponseCachingExtension.class);

        if(cacheExtensions.size() > 1) {
            throw new IllegalArgumentException("Only one cache extension may be configured");
        }

        final ResponseCachingExtension cacheManager = cacheExtensions
                .stream()
                .findFirst()
                .orElseGet(() ->
                {
                    final DefaultResponseCachingExtension responseCaching = new DefaultResponseCachingExtension();
                    responseCaching.setObjectCloner(objectCloner);
                    responseCaching.setRuntime(this);
                    responseCaching.setMessageSerializer(messageSerializer);
                    return responseCaching;
                });

        hosting.setNodeType(mode == StageMode.HOST ? NodeCapabilities.NodeTypeEnum.SERVER : NodeCapabilities.NodeTypeEnum.CLIENT);
        execution.setRuntime(this);
        execution.setObjects(objects);
        execution.setExecutionSerializer(executionSerializer);
        execution.setInvocationHandler(invocationHandler);

        messaging.setRuntime(this);

        hosting.setStage(this);
        hosting.setClusterPeer(clusterPeer);

        final NodeSelectorExtension nodeSelector = getAllExtensions(NodeSelectorExtension.class)
                .stream()
                .findFirst()
                .orElse(new RandomSelectorExtension());
        hosting.setNodeSelector(nodeSelector);

        // caches responses
        pipeline.addLast(DefaultHandlers.CACHING, cacheManager);

        pipeline.addLast(DefaultHandlers.EXECUTION, execution);

        // handles invocation messages and request-response matching
        pipeline.addLast(DefaultHandlers.HOSTING, hosting);

        // handles invocation messages and request-response matching
        pipeline.addLast(DefaultHandlers.MESSAGING, messaging);

        if (enableMessageLoopback)
        {
            final MessageLoopback messageLoopback = new MessageLoopback();
            messageLoopback.setCloner(messageLoopbackObjectCloner != null ? messageLoopbackObjectCloner : new KryoSerializer());
            messageLoopback.setRuntime(this);
            pipeline.addLast(messageLoopback.getName(), messageLoopback);
        }

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

        if (extensions.stream().noneMatch(p -> p instanceof LifetimeExtension))
        {
            extensions.add(new DefaultLifetimeExtension());
        }

        if (extensions.stream().noneMatch(p -> p instanceof ActorConstructionExtension))
        {
            extensions.add(new DefaultActorConstructionExtension());
        }

        logger.debug("Starting messaging...");
        messaging.start();
        logger.debug("Starting hosting...");
        hosting.start();
        logger.debug("Starting execution...");
        execution.start();

        logger.debug("Starting extensions...");
        await(Task.allOf(extensions.stream().map(Startable::start)));

        Task<Void> future = pipeline.connect(null);

        future = future.thenRun(() -> {
            bind();

            registerObserver(RuntimeActions.class, "", this);

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

            if (mode == StageMode.HOST)
            {
                startReminderController();
            }
        });

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

        logger.info("Stage started [{}]", runtimeIdentity());

        if (enableShutdownHook)
        {
            if (shutdownHook == null)
            {
                shutdownHook = new Thread(() ->
                {
                    synchronized (shutdownLock)
                    {
                        if (state == NodeCapabilities.NodeState.RUNNING)
                        {
                            this.doStop().join();
                        }
                    }
                });
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            }
        }

        return Task.done();
    }

    private void startReminderController()
    {
        if(useReminderShards())
        {
            IntStream.range(0, numReminderControllers).forEach(i ->
                    Actor.getReference(ShardedReminderController.class, Integer.toString(i)).ensureStart());
        }
        else
        {
            Actor.getReference(ReminderController.class).ensureStart();
        }
    }

    private boolean useReminderShards()
    {
        return numReminderControllers > 1;
    }

    public String getReminderControllerIdentity(final String reminderName)
    {
        return Integer.toString((Math.abs(reminderName.hashCode())) % numReminderControllers);
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


    @Override
    public Task<?> stop()
    {
        if(shutdownHook != null) {
            try
            {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                shutdownHook = null;
            }
            catch (IllegalStateException ex)
            {
                // VM is already shutting down so just eat the error
            }
        }

        return doStop();
    }

    private Task<?> doStop()
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

        logger.debug("Start stopping pipeline");

        // shutdown must continue in non-execution pool thread to prevent thread waiting for itself when checking executionSerializer is busy
        await(hosting.notifyStateChange().whenCompleteAsync((r, e) -> {}, shutdownExecutor));

        logger.debug("Stopping actors");
        await(stopActors().whenCompleteAsync((r, e) -> {}, shutdownExecutor));

        logger.debug("Stopping timers");
        await(stopTimers());

        do
        {
            InternalUtils.sleep(250);
        } while (executionSerializer.isBusy());

        logger.debug("Closing pipeline");
        await(pipeline.close());

        logger.debug("Stopping execution serializer");
        executionSerializer.shutdown();

        logger.debug("Stopping extensions");
        await(stopExtensions());

        state = NodeCapabilities.NodeState.STOPPED;
        logger.debug("Stop done");



        return Task.done();
    }

    private Task<Void> stopActors()
    {
        return localObjectsCleaner.shutdown();
    }

    private Task<Void> stopTimers()
    {
        try
        {
            timer.cancel();
        }
        catch (final Throwable ex)
        {
            logger.error("Error stopping timers", ex);
        }
        return Task.done();
    }

    private Task<Void> stopExtensions()
    {
        for (final ActorExtension e : getExtensions())
        {
            try
            {
                await(e.stop());
            }
            catch (final Throwable ex)
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

        return clusterPeer != null ? clusterPeer : (clusterPeer = constructDefaultClusterPeer());
    }

    public Task pulse()
    {
        if (mode == StageMode.HOST)
        {
            startReminderController();
        }
        await(clusterPeer.pulse());
        return cleanup();
    }


    public Task cleanup()
    {
        await(execution.cleanup());
        await(localObjectsCleaner.cleanup());
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
    @Override
    public void bind()
    {
        ActorRuntime.setRuntime(this.cachedRef);
    }

    private ClusterPeer constructDefaultClusterPeer()
    {
        try
        {
            final Class jGroupsClusterPeer = Class.forName("cloud.orbit.actors.cluster.JGroupsClusterPeer");
            return (ClusterPeer) jGroupsClusterPeer.getConstructors()[0].newInstance();
        }
        catch(Exception e)
        {
            throw new UncheckedException(e);
        }
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
            Map<String, Object> headers = null;
            for (final String key : stickyHeaders)
            {
                final Object value = context.getProperty(key);
                if (value != null)
                {
                    if (headers == null)
                    {
                        headers = new HashMap<>();
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
                if (localActor.isDeactivated() || state == NodeCapabilities.NodeState.STOPPED)
                {
                    cancel();
                    return;
                }

                executionSerializer.offerJob(key,
                        () -> {
                            if (localActor.isDeactivated() || state == NodeCapabilities.NodeState.STOPPED)
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
                                catch (final Exception ex)
                                {
                                    logger.warn("Error calling timer", ex);
                                }
                            }
                            return (Task) Task.done();
                        }, 10000);
            }

            @Override
            public boolean cancel()
            {
                canceled = true;
                return super.cancel();
            }
        };

        final MyRegistration registration = new MyRegistration();
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
        final Date date = new Date(clock.millis() + timeUnit.toMillis(dueTime));

        if(useReminderShards())
        {
            final String id = getReminderControllerIdentity(reminderName);
            return Actor.getReference(ShardedReminderController.class, id).registerOrUpdateReminder(actor, reminderName, date, period, timeUnit);
        }

        return Actor.getReference(ReminderController.class).registerOrUpdateReminder(actor, reminderName, date, period, timeUnit);
    }

    @Override
    public Task<?> unregisterReminder(final Remindable actor, final String reminderName)
    {
        if(useReminderShards())
        {
            final String id = getReminderControllerIdentity(reminderName);
            return Actor.getReference(ShardedReminderController.class, id).unregisterReminder(actor, reminderName);
        }

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

    @Override
    public <T> T getReference(BasicRuntime runtime, NodeAddress address, Class<T> iClass, Object id)
    {
        return DefaultDescriptorFactory.get().getReference(this, address, iClass, id);
    }

    @Override
    public StreamProvider getStreamProvider(final String providerName)
    {
        final StreamProvider streamProvider = getAllExtensions(StreamProvider.class).stream()
                .filter(p -> StringUtils.equals(p.getName(), providerName))
                .findFirst().orElseThrow(() -> new UncheckedException(String.format("Provider: %s not found", providerName)));

        final AbstractActor<?> actor = ActorTaskContext.currentActor();
        if (actor != null)
        {
            @SuppressWarnings("unchecked")
            final ActorEntry<AbstractActor> actorEntry = (ActorEntry<AbstractActor>) objects.findLocalActor((Actor) actor);

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

                            final Task<StreamSubscriptionHandle<T>> subscriptionTask = stream.subscribe(new AsyncObserver<T>()
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

    @Override
    public Task deactivateActor(Actor actor) {
        return localObjectsCleaner.deactivateActor(actor);
    }

    @Override
    public Task<Long> getActorCount() {
        return Task.fromValue(objects.getLocalActorCount());
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
