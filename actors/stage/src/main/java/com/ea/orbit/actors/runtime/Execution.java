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

package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.Remindable;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.annotation.StatelessWorker;
import com.ea.orbit.actors.annotation.StorageExtension;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.extensions.ActorClassFinder;
import com.ea.orbit.actors.extensions.ActorExtension;
import com.ea.orbit.actors.extensions.DefaultLoggerExtension;
import com.ea.orbit.actors.extensions.LifetimeExtension;
import com.ea.orbit.actors.extensions.LoggerExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.transactions.TransactionUtils;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.annotation.OnlyIfActivated;
import com.ea.orbit.annotation.Wired;
import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Container;
import com.ea.orbit.container.Startable;
import com.ea.orbit.exception.UncheckedException;

import com.google.common.collect.MapMaker;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.ea.orbit.async.Await.await;

public class Execution extends AbstractExecution
{
    private ActorClassFinder finder;
    private ConcurrentMap<Class<?>, InterfaceDescriptor> descriptorMapByInterface = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, InterfaceDescriptor> descriptorMapByInterfaceId = new ConcurrentHashMap<>();
    private Map<EntryKey, ReferenceEntry> localActors = new ConcurrentHashMap<>();
    private Map<EntryKey, ActorObserver> observerInstances = new MapMaker().weakValues().makeMap();

    // from implementation to reference
    private Map<ActorObserver, ActorObserver> observerReferences = new MapMaker().weakKeys().makeMap();

    private Hosting hosting;
    private Timer timer = new Timer("Orbit stage timer");
    private Clock clock = Clock.systemUTC();
    private long cleanupIntervalMillis = TimeUnit.MINUTES.toMillis(5);
    private ExecutorService executor;
    private ActorFactoryGenerator dynamicReferenceFactory = new ActorFactoryGenerator();

    private List<ActorExtension> extensions = new ArrayList<>();

    private NodeCapabilities.NodeState state = NodeCapabilities.NodeState.RUNNING;


    private Stage stage;
    /**
     * RPC message headers that are copied from and to the TaskContext.
     * <p>
     * These fields are copied from the TaskContext to the message headers when sending messages.
     * And from the message header to the TaskContext when receiving them.
     * </p>
     */

    @Wired
    private Container container;

    private ActorRuntime runtime;

    @Config("orbit.actors.stickyHeaders")
    private Set<String> stickyHeaders = new HashSet<>(Arrays.asList(TransactionUtils.ORBIT_TRANSACTION_ID, "orbit.traceId"));
    private LoggerExtension loggerExtension;

    public Execution()
    {
        // the last runtime created will be the default.
    }


    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public void setExecutor(final ExecutorService executor)
    {
        this.executor = executor;
    }

    public void addStickyHeaders(Collection<String> stickyHeaders)
    {
        this.stickyHeaders.addAll(stickyHeaders);
    }

    public void setRuntime(final ActorRuntime runtime)
    {
        this.runtime = runtime;
    }

    public ExecutorService getExecutor()
    {
        return executor;
    }

    public boolean canActivateActor(String interfaceName)
    {
        if (state != NodeCapabilities.NodeState.RUNNING)
        {
            return false;
        }
        Class<Actor> aInterface = classForName(interfaceName);
        final InterfaceDescriptor descriptor = getDescriptor(aInterface);
        if (descriptor == null || descriptor.cannotActivate)
        {
            return false;
        }
        if (descriptor.concreteClassName == null)
        {
            final Class<?> concreteClass = finder.findActorImplementation(aInterface);
            descriptor.cannotActivate = concreteClass == null;
            descriptor.concreteClassName = concreteClass != null ? concreteClass.getName() : null;
            if (concreteClass != null)
            {
                descriptor.invoker = dynamicReferenceFactory.getInvokerFor(concreteClass);
            }
        }
        return !descriptor.cannotActivate;
    }

    public void registerFactory(ReferenceFactory<?> factory)
    {
        // TODO: will enable caching the reference factory
    }

    public Stage getStage()
    {
        return stage;
    }

    public void setStage(Stage stage)
    {
        this.stage = stage;
    }

    private static class InterfaceDescriptor
    {
        ReferenceFactory<?> factory;
        volatile ObjectInvoker<Object> invoker;
        boolean cannotActivate;
        String concreteClassName;
        boolean isObserver;
        volatile ObjectInvoker<Object> interfaceInvoker;

        @Override
        public String toString()
        {
            return this.concreteClassName;
        }
    }

    private class ReferenceEntry
    {
        RemoteReference<?> reference;
        InterfaceDescriptor descriptor;
        boolean statelessWorker;
        Activation singleActivation;

        ConcurrentLinkedDeque<Activation> statelessActivations;

        boolean removable;

        public void pushActivation(final Activation activation)
        {
            if (!statelessWorker)
            {
                if (singleActivation != null)
                {
                    logger.error("There should be only one single activation! Reference: {}", reference);
                }
                singleActivation = activation;
            }
            else
            {
                statelessActivations.offerLast(activation);
            }
        }

        public Activation popActivation()
        {
            Activation activation;
            if (!statelessWorker)
            {
                activation = singleActivation;
                singleActivation = null;
                return (activation != null) ? activation : new Activation(this, null);
            }
            activation = statelessActivations.pollLast();
            return (activation != null) ? activation : new Activation(this, null);
        }

        public Activation peekOldActivation()
        {
            if (!statelessWorker)
            {
                return singleActivation;
            }
            else
            {
                return statelessActivations.peekFirst();
            }
        }

        public Task<?> cleanup(final EntryKey key, long cutOut)
        {
            if (localActors.get(key) != this)
            {
                logger.warn("Error during cleanup: the ActivationEntry changed. This should not be possible. {}", key);
                return Task.done();
            }
            if (!statelessWorker)
            {
                if (singleActivation != null)
                {
                    if (singleActivation.instance != null)
                    {
                        if (singleActivation.lastAccess > cutOut)
                        {
                            // has been used recently enough. not disposing.
                            return Task.done();
                        }
                        // TODO deactivation code
                        if (singleActivation.instance instanceof AbstractActor)
                        {
                            try
                            {
                                bind();
                                AbstractActor<?> actor = (AbstractActor<?>) singleActivation.instance;
                                Task.allOf(getAllExtensions(LifetimeExtension.class).stream().map(v -> v.preDeactivation(actor)))
                                        .thenCompose(() -> actor.deactivateAsync())
                                        .thenCompose(() -> Task.allOf(getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postDeactivation(actor))))
                                        .thenRun(() -> {
                                            singleActivation.instance = null;
                                            localActors.remove(key);
                                        });

                            }
                            catch (Exception e)
                            {
                                if (logger.isErrorEnabled())
                                {
                                    logger.error("Error during the clean up. " + key, e);
                                }
                            }
                        }
                        singleActivation.instance = null;
                    }
                }
                localActors.remove(key);
                return Task.done();
            }
            else
            {
                int count = statelessActivations.size();
                List<Task<?>> futures = new ArrayList<>();
                for (int i = 0; i < count; i++)
                {
                    Activation activation = statelessActivations.pollFirst();
                    if (activation == null)
                    {
                        break;
                    }
                    if (activation.lastAccess > cutOut)
                    {
                        // return it
                        statelessActivations.addLast(activation);
                    }
                    else
                    {
                        if (activation.instance instanceof AbstractActor)
                        {
                            try
                            {
                                bind();
                                AbstractActor<?> actor = (AbstractActor<?>) activation.instance;
                                Task.allOf(getAllExtensions(LifetimeExtension.class).stream().map(v -> v.preDeactivation(actor)))
                                        .thenCompose(() -> actor.deactivateAsync())
                                        .thenCompose(() -> Task.allOf(getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postDeactivation(actor))))
                                        .thenRun(() -> {
                                            activation.instance = null;
                                        });
                            }
                            catch (Exception e)
                            {
                                if (logger.isErrorEnabled())
                                {
                                    logger.error("Error during the clean up. " + key, e);
                                }
                                activation.instance = null;
                            }
                        }
                    }
                }
                if (futures.size() > 0)
                {
                    return Task.allOf(futures);
                }
                // TODO figure out how to safely remove the entry
            }
            return Task.done();
        }
    }

    private void bind()
    {
        runtime.bind();
    }

    private class Activation
    {
        ReferenceEntry entry;
        long lastAccess = clock.millis();
        Object instance;

        public Activation(final ReferenceEntry entry, final Object instance)
        {
            this.entry = entry;
            this.instance = instance;
        }

        // gets or creates the instance
        // gets or creates the instance
        public Task<Object> getOrCreateInstance()
        {
            try
            {
                if (instance == null)
                {
                    Object newInstance = classForName(entry.descriptor.concreteClassName).newInstance();
                    if (newInstance instanceof AbstractActor)
                    {
                        final AbstractActor<?> actor = (AbstractActor<?>) newInstance;
                        ActorTaskContext.current().setActor(actor);
                        actor.reference = entry.reference;
                        actor.runtime = stage;
                        actor.logger = loggerExtension.getLogger(actor);
                        actor.stateExtension = getStorageExtensionFor(actor);

                        await(Task.allOf(getAllExtensions(LifetimeExtension.class).stream().map(v -> v.preActivation(actor))));

                        if (actor.stateExtension != null)
                        {
                            try
                            {
                                await(actor.readState());
                            }
                            catch (Exception ex)
                            {
                                if (logger.isErrorEnabled())
                                {
                                    logger.error("Error reading actor state for: " + entry.reference, ex);
                                }
                                throw ex;
                            }
                        }
                        await(actor.activateAsync());
                        await(Task.allOf(getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postActivation(actor))));
                        instance = newInstance;
                    }
                }

                return Task.fromValue(instance);
            }
            catch (Exception e)
            {
                throw new UncheckedException(e);
            }
        }
    }

    public void setExtensions(List<ActorExtension> extensions)
    {
        this.extensions = extensions;
    }

    @SuppressWarnings("unchecked")
    public <T extends ActorExtension> T getFirstExtension(Class<T> itemType)
    {
        return (extensions == null) ? null :
                (T) extensions.stream().filter(p -> itemType.isInstance(p)).findFirst().orElse(null);

    }

    @SuppressWarnings("unchecked")
    public <T extends ActorExtension> List<T> getAllExtensions(Class<T> itemType)
    {
        return extensions == null ? Collections.emptyList()
                : (List<T>) extensions.stream().filter(p -> itemType.isInstance(p)).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends ActorExtension> T getStorageExtensionFor(AbstractActor actor)
    {
        if (extensions == null)
        {
            return null;
        }
        StorageExtension ann = actor.getClass().getAnnotation(StorageExtension.class);
        String extensionName = ann == null ? "default" : ann.value();

        // selects the fist provider with the right name
        return (T) extensions.stream()
                .filter(p -> (p instanceof com.ea.orbit.actors.extensions.StorageExtension) && extensionName.equals(((com.ea.orbit.actors.extensions.StorageExtension) p).getName()))
                .findFirst()
                .orElse(null);
    }

    public void setHosting(final Hosting hosting)
    {
        this.hosting = hosting;
    }

    public Task<?> stop()
    {
        // * refuse new actor activations
        state = NodeCapabilities.NodeState.STOPPING;
        await(hosting.notifyStateChange());

        // * deactivate all actors
        await(activationCleanup());

        // * finalize all timers
        timer.cancel();

        // * give extensions a chance to send a message
        await(Task.allOf(extensions.stream().map(StageLifecycleListener::onPreStop)));

        // * stop processing new received messages (responses still work)
        // * notify rest of the cluster (no more observer messages)
        state = NodeCapabilities.NodeState.STOPPED;
        await(hosting.notifyStateChange());

        // * wait pending tasks execution
        executionSerializer.shutdown();

        // * cancel all pending messages, and prevents sending new ones
        //await(messaging.stop());

        // ** stop all extensions
        await(Task.allOf(extensions.stream().map(Startable::stop)));

        return Task.done();
    }

    public <T extends ActorObserver> T getObjectReference(final Class<T> iClass, final T observer)
    {
        return getObserverReference(iClass, observer, null);
    }

    /**
     * Installs this observer into this node with the given id.
     * If called twice for the same observer, the ids must match.
     * Usually it's recommended to let the framework choose the id;
     *
     * @param iClass   hint to the framework about which ActorObserver interface this object represents.
     *                 Can be null if there are no ambiguities.
     * @param observer the object to install
     * @param id       can be null, in this case the framework will choose an id.
     * @param <T>      The type of reference class returned.
     * @return a remote reference that can be sent to actors.
     * @throws java.lang.IllegalArgumentException if called twice with the same observer and different ids
     */
    @SuppressWarnings("unchecked")
    public <T extends ActorObserver> T getObserverReference(Class<T> iClass, final T observer, String id)
    {
        final ActorObserver ref = observerReferences.get(observer);
        if (ref != null)
        {
            if (id != null && !id.equals(((RemoteReference<?>) ref).id))
            {
                throw new IllegalArgumentException("Called twice with different ids: " + id + " != " + ((RemoteReference<?>) ref).id);
            }
            return (T) ref;
        }
        return createObjectReference(iClass, observer, id);
    }

    @SuppressWarnings("unchecked")
    private <T extends ActorObserver> T createObjectReference(final Class<T> iClass, final T observer, String objectId)
    {

        ReferenceFactory<?> factory;
        if (iClass == null)
        {
            factory = findFactoryFor(ActorObserver.class, observer);
        }
        else
        {
            factory = getDescriptor(iClass).factory;
        }
        if (factory == null)
        {
            throw new UncheckedException("Can't find factory for " + observer.getClass());
        }
        final String id = objectId != null ? objectId : UUID.randomUUID().toString();

        EntryKey key = new EntryKey(factory.getInterfaceId(), id);
        final ActorObserver existingObserver = observerInstances.get(key);
        if (existingObserver == null)
        {
            final RemoteReference<T> reference = (RemoteReference<T>) factory.createReference(id);
            if (objectId == null)
            {
                reference.address = hosting.getNodeAddress();
            }
            reference.runtime = Execution.this.runtime;
            observerInstances.putIfAbsent(key, observer);
            observerReferences.putIfAbsent(observer, (ActorObserver) reference);
            return (T) reference;
        }
        else if (observer != existingObserver)
        {
            throw new IllegalArgumentException("ActorObserver id clashes with a pre existing observer: " + id);
        }
        return (T) observerReferences.get(observer);
    }

    public Registration registerTimer(final AbstractActor<?> actor,
                                      final Callable<Task<?>> taskCallable,
                                      final long dueTime, final long period,
                                      final TimeUnit timeUnit)
    {
        // TODO: handle deactivation.
        final TimerTask timerTask = new TimerTask()
        {
            boolean canceled;

            @Override
            public void run()
            {
                // TODO decide if it's necessary to change the key here for the actor activation?
                executionSerializer.offerJob(actor,
                        () -> {
                            bind();
                            try
                            {
                                if (!canceled)
                                {
                                    return taskCallable.call();
                                }
                            }
                            catch (Exception ex)
                            {
                                logger.warn("Error calling timer", ex);
                            }
                            return Task.done();
                        }, 1000);
            }

            @Override
            public boolean cancel()
            {
                canceled = true;
                return super.cancel();
            }
        };

        if (period > 0)
        {
            timer.schedule(timerTask, timeUnit.toMillis(dueTime), timeUnit.toMillis(period));
        }
        else
        {
            timer.schedule(timerTask, timeUnit.toMillis(dueTime));
        }

        return timerTask::cancel;
    }

    public Task<?> registerReminder(final Remindable actor, final String reminderName, final long dueTime, final long period, final TimeUnit timeUnit)
    {
        return getReference(ReminderController.class, "0").registerOrUpdateReminder(actor, reminderName, new Date(clock.millis() + timeUnit.toMillis(dueTime)), period, timeUnit);
    }

    public Task<?> unregisterReminder(final Remindable actor, final String reminderName)
    {
        return getReference(ReminderController.class, "0").unregisterReminder(actor, reminderName);
    }

    public void start()
    {
        if (loggerExtension == null)
        {
            loggerExtension = getFirstExtension(LoggerExtension.class);
            if (loggerExtension == null)
            {
                loggerExtension = new DefaultLoggerExtension();
            }
        }

        finder = getFirstExtension(ActorClassFinder.class);
        if (finder == null)
        {
            finder = new DefaultActorClassFinder();
            finder.start().join();
        }

        if (container != null)
        {
            // pre create the class descriptors if possible.
            container.getClasses().stream()
                    .filter(c -> (c.isInterface() && Actor.class.isAssignableFrom(c)))
                    .parallel()
                    .forEach(c -> getDescriptor(c));
        }

        getDescriptor(NodeCapabilities.class);
        createObjectReference(NodeCapabilities.class, hosting, "");

        if (executor == null)
        {
            executor = ExecutorUtils.newScalingThreadPool(64);
        }
        executionSerializer = new ExecutionSerializer<>(executor);

        extensions.forEach(Startable::start);
        // schedules the cleanup
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (state == NodeCapabilities.NodeState.RUNNING)
                {
                    ForkJoinTask.adapt(() -> activationCleanup().join()).fork();
                }
            }
        }, cleanupIntervalMillis, cleanupIntervalMillis);
    }

    private <T> Class<T> classForName(final String className)
    {
        return classForName(className, false);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> classForName(final String className, boolean ignoreException)
    {
        try
        {
            return (Class<T>) Class.forName(className);
        }
        catch (Error | Exception ex)
        {
            if (!ignoreException)
            {
                throw new Error("Error loading class: " + className, ex);
            }
        }
        return null;
    }

    private ReferenceFactory<?> findFactoryFor(final Class<?> baseInterface, final Object instance)
    {
        for (Class<?> aInterface : instance.getClass().getInterfaces())
        {
            if (baseInterface.isAssignableFrom(aInterface))
            {
                ReferenceFactory<?> factory = getDescriptor(aInterface).factory;
                if (factory != null)
                {
                    return factory;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private InterfaceDescriptor getDescriptor(final Class<?> aInterface)
    {
        InterfaceDescriptor interfaceDescriptor = descriptorMapByInterface.get(aInterface);
        if (interfaceDescriptor == null)
        {
            if (aInterface == Actor.class || aInterface == ActorObserver.class || !aInterface.isInterface())
            {
                return null;
            }

            interfaceDescriptor = new InterfaceDescriptor();
            interfaceDescriptor.isObserver = ActorObserver.class.isAssignableFrom(aInterface);
            interfaceDescriptor.factory = dynamicReferenceFactory.getFactoryFor(aInterface);
            interfaceDescriptor.invoker = (ObjectInvoker<Object>) interfaceDescriptor.factory.getInvoker();

            InterfaceDescriptor concurrentInterfaceDescriptor = descriptorMapByInterface.putIfAbsent(aInterface, interfaceDescriptor);
            if (concurrentInterfaceDescriptor != null)
            {
                descriptorMapByInterfaceId.put(interfaceDescriptor.factory.getInterfaceId(), concurrentInterfaceDescriptor);
                return concurrentInterfaceDescriptor;
            }


            descriptorMapByInterfaceId.put(interfaceDescriptor.factory.getInterfaceId(), interfaceDescriptor);
        }
        return interfaceDescriptor;
    }

    private InterfaceDescriptor getDescriptor(final int interfaceId)
    {
        return descriptorMapByInterfaceId.get(interfaceId);
    }

    @Override
    public void onRead(HandlerContext ctx, Object msg)
    {
        final Message message = (Message) msg;
        final int messageType = message.getMessageType();
        final NodeAddress fromNode = message.getFromNode();

        onMessageReceived(message)
                .whenComplete((r, e) ->
                        sendResponseAndLogError(ctx,
                                messageType == MessageDefinitions.ONE_WAY_MESSAGE,
                                fromNode, message.getMessageId(), r, e));

    }

    protected void sendResponseAndLogError(HandlerContext ctx, boolean oneway, final NodeAddress from, int messageId, Object result, Throwable exception)
    {
        if (exception != null && logger.isDebugEnabled())
        {
            logger.debug("Unknown application error. ", exception);
        }

        if (!oneway)
        {
            if (exception == null)
            {
                sendResponse(ctx, from, MessageDefinitions.RESPONSE_OK, messageId, result);
            }
            else
            {
                sendResponse(ctx, from, MessageDefinitions.RESPONSE_ERROR, messageId, exception);
            }
        }
    }

    private Task sendResponse(HandlerContext ctx, NodeAddress to, int messageType, int messageId, Object res)
    {
        return ctx.write(new Message()
                .withToNode(to)
                .withMessageId(messageId)
                .withMessageType(messageType)
                .withPayload(res));
    }


    // this method is executed serially by entryKey
    protected Task<?> handleOnMessageReceived(final Task completion,
                                              final EntryKey entryKey, final Message message)
    {
        messagesHandled.increment();
        final InterfaceDescriptor descriptor = getDescriptor(message.getInterfaceId());
        if (descriptor.isObserver)
        {
            final ActorObserver observer = observerInstances.get(entryKey);
            if (observer == null)
            {
                completion.completeExceptionally(new UncheckedException("Observer no longer present"));
                return Task.done();
            }
            if (descriptor.invoker == null)
            {
                descriptor.invoker = dynamicReferenceFactory.getInvokerFor(observer.getClass());
            }
            return descriptor.invoker.safeInvoke(observer, message.getMethodId(), (Object[]) message.getPayload())
                    .handle((r, e) -> {
                        if (e == null)
                        {
                            completion.complete(r);
                        }
                        else
                        {
                            completion.completeExceptionally(e);
                        }
                        return null;
                    });
        }

        ReferenceEntry entry = localActors.get(entryKey);
        Object key = message.getObjectId();

        if (logger.isDebugEnabled())
        {
            logger.debug("handleOnMessageReceived for: " + descriptor + ":" + key);
        }

        if (entry == null)
        {
            // TODO check if this is the activation node. Otherwise forward to the activation node.
            entry = new ReferenceEntry();
            entry.descriptor = descriptor;
            entry.statelessWorker = descriptor.factory.getInterface().isAnnotationPresent(StatelessWorker.class);
            if (entry.statelessWorker)
            {
                entry.statelessActivations = new ConcurrentLinkedDeque<>();
            }
            entry.reference = (RemoteReference<?>) descriptor.factory.createReference(key != null ? String.valueOf(key) : null);
            entry.reference.runtime = this.runtime;
            entry.removable = true;

            ReferenceEntry old = localActors.putIfAbsent(entryKey, entry);
            if (old != null)
            {
                // this should be impossible if all accesses are serial.
                logger.error("Unexpected state: Non serial access to entry!");
                entry = old;
            }
        }

        final ReferenceEntry theEntry = entry;
        if (!entry.statelessWorker)
        {
            return executeMessage(completion, theEntry, descriptor, message);
        }
        else
        {
            if (!executionSerializer.offerJob(null,
                    () -> executeMessage(completion, theEntry, descriptor, message),
                    maxQueueSize))
            {
                refusedExecutions.increment();
                if (logger.isErrorEnabled())
                {
                    logger.info("Execution refused: " + key + ":" + message.getInterfaceId() + ":" + message.getMethodId() + ":" + message.getMessageId());
                }
                completion.completeExceptionally(new UncheckedException("Execution refused"));
            }
            return Task.done();
        }
    }


    private Task<Void> executeMessage(
            final Task completion,
            final ReferenceEntry theEntry,
            final InterfaceDescriptor descriptor,
            final Message message)
    {
        final ActorTaskContext context = ActorTaskContext.pushNew();
        try
        {
            context.setProperty(ActorRuntime.class.getName(), this);
            Activation activation = theEntry.popActivation();
            activation.lastAccess = clock.millis();
            if (message.getHeaders() instanceof Map)
            {
                @SuppressWarnings("unchecked")
                final Map<Object, Object> headersMap = (Map) message.getHeaders();
                for (Map.Entry<Object, Object> e : headersMap.entrySet())
                {
                    if (stickyHeaders.contains(e.getKey()))
                    {
                        context.setProperty((String) e.getKey(), e.getValue());
                    }
                }
            }
            try
            {
                bind();
                final Object actor = await(activation.getOrCreateInstance());
                context.setActor((AbstractActor<?>) actor);
                if (descriptor.invoker == null)
                {
                    descriptor.invoker = dynamicReferenceFactory.getInvokerFor(actor.getClass());
                }
                return descriptor.invoker.safeInvoke(actor, message.getMethodId(), (Object[]) message.getPayload())
                        .handle((r, e) -> {
                            if (e == null)
                            {
                                completion.complete(r);
                            }
                            else
                            {
                                completion.completeExceptionally(e);
                            }
                            return null;
                        });
            }
            finally
            {
                // we don't need to unset the ActorRuntime, @see ActorRuntime.setRuntime:
                theEntry.pushActivation(activation);
            }
        }
        catch (Exception ex)
        {
            completion.completeExceptionally(ex);
        }
        finally
        {
            context.pop();
        }
        return Task.done();
    }


    @SuppressWarnings({ "unchecked" })
    <T> T createReference(final NodeAddress a, final Class<T> iClass, String id)
    {
        final InterfaceDescriptor descriptor = getDescriptor(iClass);
        RemoteReference<?> reference = (RemoteReference<?>) descriptor.factory.createReference("");
        reference.address = a;
        reference.runtime = this.runtime;
        reference.id = id;
        return (T) reference;
    }


    @SuppressWarnings("unchecked")
    public <T extends Actor> T getReference(final Class<T> iClass, final Object id)
    {
        final InterfaceDescriptor descriptor = getDescriptor(iClass);
        RemoteReference<?> reference = (RemoteReference<?>) descriptor.factory.createReference(id != null ? String.valueOf(id) : null);
        reference.runtime = this.runtime;
        return (T) reference;
    }

    public ObjectInvoker<?> getInvoker(final int interfaceId)
    {
        final InterfaceDescriptor descriptor = getDescriptor(interfaceId);
        if (descriptor == null)
        {
            return null;
        }
        if (descriptor.interfaceInvoker == null)
        {
            descriptor.interfaceInvoker = dynamicReferenceFactory.getInvokerFor(descriptor.factory.getInterface());
        }
        return descriptor.interfaceInvoker;
    }

    @SuppressWarnings("unchecked")
    public <T extends ActorObserver> T getRemoteObjectReference(NodeAddress address, final Class<T> iClass, final Object id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("Null id for " + iClass);
        }
        if (iClass == null)
        {
            throw new IllegalArgumentException("Null class");
        }
        final InterfaceDescriptor descriptor = getDescriptor(iClass);
        RemoteReference<?> reference = (RemoteReference<?>) descriptor.factory.createReference(String.valueOf(id));
        reference.runtime = this.runtime;
        reference.address = address;
        return (T) reference;
    }


    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            return invoke(ctx, (Invocation) msg);
        }
        return super.write(ctx, msg);
    }

    public Task<?> invoke(final HandlerContext ctx, Invocation invocation)
    {
        final Method method = invocation.getMethod();
        final Addressable toReference = invocation.getToReference();
        if (!verifyActivated(toReference, method))
        {
            return Task.done();
        }
        final Task<?> task;
        if (invocation.getToNode() == null)
        {

            NodeAddress address;
            if (toReference instanceof RemoteReference
                    && (address = RemoteReference.getAddress((RemoteReference) toReference)) != null)
            {
                invocation.withToNode(address);
                task = ctx.write(invocation);
            }
            else
            {
                final Addressable actorReference = toReference;
                // TODO: Ensure that both paths encode exception the same way.
                task = hosting.locateActor(actorReference, true)
                        .thenCompose(x -> {
                            return ctx.write(invocation
                                    .withToNode(x)
                                    .withFromNode(hosting.getNodeAddress()));
                        });
            }
        }
        else
        {
            task = ctx.write(invocation);
        }
        return task.whenCompleteAsync((r, e) ->
                {
                    // place holder, just to ensure the completion happens in another thread
                },
                executor);
    }

    public Task<?> activationCleanup()
    {

        long cutOut = clock.millis() - TimeUnit.MINUTES.toMillis(10);
        final List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Iterator<Map.Entry<EntryKey, ReferenceEntry>> iterator = localActors.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry<EntryKey, ReferenceEntry> mEntry = iterator.next();
            final ReferenceEntry entry = mEntry.getValue();
            if (!entry.removable)
            {
                continue;
            }
            Activation act = entry.peekOldActivation();
            if (act == null && state == NodeCapabilities.NodeState.RUNNING)
            {
                continue;
            }
            if (state != NodeCapabilities.NodeState.RUNNING || act.lastAccess < cutOut)
            {
                CompletableFuture<Object> future = new CompletableFuture<>();
                final Supplier<Task<?>> task = () -> {
                    try
                    {
                        final Task<?> res = entry.cleanup(mEntry.getKey(), cutOut);
                        if (res != null && !res.isDone())
                        {
                            res.whenComplete((r, e) -> {
                                if (e != null)
                                {
                                    future.completeExceptionally(e);
                                }
                                else
                                {
                                    future.complete(r);
                                }
                            });
                        }
                        else
                        {
                            future.complete(null);
                        }
                        return res;
                    }
                    catch (Error | RuntimeException ex)
                    {
                        future.completeExceptionally(ex);
                        throw ex;
                    }
                    catch (Throwable ex)
                    {
                        future.completeExceptionally(ex);
                        throw new UncheckedException(ex);
                    }
                };
                if (executionSerializer.offerJob(mEntry.getKey(), task, maxQueueSize))
                {
                    futures.add(future);
                }
            }
        }
        Task<Void> listTask = Task.allOf(futures);
        return listTask;
    }

    public Task<NodeAddress> locateActor(final Addressable actorReference, final boolean forceActivation)
    {
        return hosting.locateActor(actorReference, false);
    }

    public NodeCapabilities.NodeState getState()
    {
        return state;
    }

    public long getLocalActorCount()
    {
        return localActors.size();
    }

    public long getMessagesReceivedCount()
    {
        return messagesReceived.longValue();
    }

    public long getMessagesHandledCount()
    {
        return messagesHandled.longValue();
    }

    public long getRefusedExecutionsCount()
    {
        return refusedExecutions.longValue();
    }

    /**
     * Checks if the method passes an Activated check.
     * Verify passes on either of:
     * - method can run only if activated, and the actor is active
     * - the method is not marked with OnlyIfActivated.
     */
    private boolean verifyActivated(Addressable toReference, Method method)
    {
        if (method.isAnnotationPresent(OnlyIfActivated.class))
        {
            NodeAddress actorAddress = locateActor(toReference, false).join();
            if (actorAddress == null)
            {
                return false;
            }
        }
        return true;
    }
}
