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

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.IActorObserver;
import com.ea.orbit.actors.IAddressable;
import com.ea.orbit.actors.IRemindable;
import com.ea.orbit.actors.annotation.StatelessWorker;
import com.ea.orbit.actors.cluster.INodeAddress;
import com.ea.orbit.actors.providers.ILifetimeProvider;
import com.ea.orbit.actors.providers.IOrbitProvider;
import com.ea.orbit.actors.providers.IStorageProvider;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.util.ClassPath;
import com.ea.orbit.util.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;

import java.lang.ref.WeakReference;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Execution implements IRuntime
{
    private static final Logger logger = LoggerFactory.getLogger(Execution.class);
    private Map<Class<?>, InterfaceDescriptor> descriptorMapByInterface = new HashMap<>();
    private Map<Integer, InterfaceDescriptor> descriptorMapByInterfaceId = new HashMap<>();
    private Map<EntryKey, ReferenceEntry> localActors = new ConcurrentHashMap<>();
    private Map<EntryKey, IActorObserver> observerInstances = new MapMaker().weakValues().makeMap();
    // from implementation to reference
    private Map<IActorObserver, IActorObserver> observerReferences = new MapMaker().weakKeys().makeMap();

    private Hosting hosting;
    private Messaging messaging;
    private ExecutionSerializer executionSerializer;
    private int maxQueueSize = 10000;
    private List<IOrbitProvider> providers = new ArrayList<>();
    private Timer timer = new Timer("Orbit stage timer");
    private Clock clock = Clock.systemUTC();
    private long cleanupIntervalMillis = TimeUnit.MINUTES.toMillis(5);
    private AtomicLong messagesReceived = new AtomicLong();
    private AtomicLong messagesHandled = new AtomicLong();
    private AtomicLong refusedExecutions = new AtomicLong();
    private ExecutorService executor;

    @Config("orbit.actors.autoDiscovery")
    private boolean autoDiscovery = true;
    private List<Class<?>> actorClasses = new ArrayList<>();
    private List<String> availableActors;
    private final WeakReference<IRuntime> cachedRef = new WeakReference<>(this);

    public Execution()
    {
        // the last runtime created will be the default.
        Runtime.runtimeCreated(cachedRef);
    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public void setExecutor(final ExecutorService executor)
    {
        this.executor = executor;
    }

    public ExecutorService getExecutor()
    {
        return executor;
    }

    private static class InterfaceDescriptor
    {
        ActorFactory factory;
        ActorInvoker invoker;
        String concreteClassName;
        boolean isObserver;

        @Override
        public String toString()
        {
            return this.concreteClassName;
        }
    }

    private class ReferenceEntry
    {
        ActorReference reference;
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
                        if (singleActivation.instance instanceof OrbitActor)
                        {
                            try
                            {

                                Runtime.setRuntime(Execution.this.cachedRef);
                                OrbitActor orbitActor = (OrbitActor) singleActivation.instance;
                                Task.allOf(getAllProviders(ILifetimeProvider.class).stream().map(v -> v.preDeactivation(orbitActor)))
                                        .thenCompose(() -> orbitActor.deactivateAsync())
                                        .thenCompose(() -> Task.allOf(getAllProviders(ILifetimeProvider.class).stream().map(v -> v.postDeactivation(orbitActor))))
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
                List<Task> futures = new ArrayList<>();
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
                        if (activation.instance instanceof OrbitActor)
                        {
                            try
                            {
                                Runtime.setRuntime(Execution.this.cachedRef);
                                OrbitActor orbitActor = (OrbitActor) activation.instance;
                                Task.allOf(getAllProviders(ILifetimeProvider.class).stream().map(v -> v.preDeactivation(orbitActor)))
                                        .thenCompose(() -> orbitActor.deactivateAsync())
                                        .thenCompose(() -> Task.allOf(getAllProviders(ILifetimeProvider.class).stream().map(v -> v.postDeactivation(orbitActor))))
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

    private static class EntryKey
    {
        int interfaceId;
        Object id;

        private EntryKey(final int interfaceId, final Object id)
        {
            this.interfaceId = interfaceId;
            this.id = id;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (!(o instanceof EntryKey)) return false;

            final EntryKey entryKey = (EntryKey) o;

            if (interfaceId != entryKey.interfaceId) return false;
            if (id != null ? !id.equals(entryKey.id) : entryKey.id != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = interfaceId;
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }

        @Override
        public String toString()
        {
            return "EntryKey{" +
                    "interfaceId=" + interfaceId +
                    ", id=" + id +
                    '}';
        }
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
        public Object getOrCreateInstance() throws IllegalAccessException, InstantiationException, ExecutionException, InterruptedException
        {
            if (instance == null)
            {
                Object newInstance = classForName(entry.descriptor.concreteClassName).newInstance();
                if (newInstance instanceof OrbitActor)
                {
                    final OrbitActor orbitActor = (OrbitActor) newInstance;
                    orbitActor.reference = entry.reference;

                    orbitActor.stateProvider = getFirstProvider(IStorageProvider.class);

                    Task.allOf(getAllProviders(ILifetimeProvider.class).stream().map(v -> v.preActivation(orbitActor))).join();

                    if (orbitActor.stateProvider != null)
                    {
                        try
                        {
                            orbitActor.readState();
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
                    instance = newInstance;

                    orbitActor.activateAsync().join();
                    Task.allOf(getAllProviders(ILifetimeProvider.class).stream().map(v -> v.postActivation(orbitActor))).join();
                }

            }
            return instance;
        }
    }

    private Map<IAddressable, ReferenceEntry> actors;


    public void setProviders(List<IOrbitProvider> providers)
    {
        this.providers = providers;
    }

    @SuppressWarnings("unchecked")
    public <T extends IOrbitProvider> T getFirstProvider(Class<T> itemType)
    {
        return (providers == null) ? null :
                (T) providers.stream().filter(p -> itemType.isInstance(p)).findFirst().orElse(null);

    }

    @SuppressWarnings("unchecked")
    public <T extends IOrbitProvider> List<T> getAllProviders(Class<T> itemType)
    {
        return providers == null ? Collections.emptyList()
                : (List<T>) providers.stream().filter(p -> itemType.isInstance(p)).collect(Collectors.toList());
    }

    public void setHosting(final Hosting hosting)
    {
        this.hosting = hosting;
    }

    public void setMessaging(final Messaging messaging)
    {
        this.messaging = messaging;
    }

    public Task stop()
    {
        timer.cancel();
        return Task.allOf(providers.stream().map(v -> v.stop()));
    }

    public <T extends IActorObserver> T getObjectReference(final Class<T> iClass, final T observer)
    {
        final IActorObserver ref = observerReferences.get(observer);
        if (ref != null)
        {
            return (T) ref;
        }
        return createObjectReference(iClass, observer, null);
    }

    private <T extends IActorObserver> T createObjectReference(final Class<T> iClass, final T observer, String objectId)
    {

        ActorFactory<T> factory;
        if (iClass == null)
        {
            factory = findFactoryFor(IActorObserver.class, observer);
        }
        else
        {
            factory = descriptorMapByInterface.get(iClass).factory;
        }
        if (factory == null)
        {
            throw new UncheckedException("Can't find factory for " + observer.getClass());
        }
        final String id = objectId != null ? objectId : UUID.randomUUID().toString();

        EntryKey key = new EntryKey(factory.getInterfaceId(), id);
        final IActorObserver existingObserver = observerInstances.get(key);
        if (existingObserver == null)
        {
            final ActorReference<T> reference = (ActorReference<T>) factory.createReference(id);
            if (objectId == null)
            {
                reference.address = messaging.getNodeAddress();
            }
            reference.runtime = Execution.this;
            observerInstances.putIfAbsent(key, observer);
            observerReferences.putIfAbsent(observer, (IActorObserver) reference);
            return (T) reference;
        }
        else if (observer != existingObserver)
        {
            throw new IllegalArgumentException("ActorObserver id clashes with a pre existing observer: " + id);
        }
        return (T) observerReferences.get(observer);
    }

    public Registration registerTimer(final OrbitActor actor,
                                      final Callable<Task<?>> taskCallable,
                                      final long dueTime, final long period,
                                      final TimeUnit timeUnit)
    {
        final TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                // TODO decide if it's necessary to change the key here for the actor activation?
                executionSerializer.offerJob(actor,
                        () -> {
                            Runtime.setRuntime(Execution.this.cachedRef);
                            try
                            {
                                return taskCallable.call();
                            }
                            catch (Exception ex)
                            {
                                logger.warn("Error calling timer", ex);
                            }
                            return Task.done();
                        }, 1000);
            }
        };
        timer.schedule(timerTask, timeUnit.toMillis(dueTime), timeUnit.toMillis(period));
        return () -> timerTask.cancel();
    }

    @Override
    public Clock clock()
    {
        return clock;
    }

    @Override
    public Task registerReminder(final IRemindable actor, final String reminderName, final long dueTime, final long period, final TimeUnit timeUnit)
    {
        return getReference(IReminderController.class, "0").registerOrUpdateReminder(actor, reminderName, new Date(clock.millis() + timeUnit.toMillis(dueTime)), period, timeUnit);
    }

    @Override
    public Task unregisterReminder(final IRemindable actor, final String reminderName)
    {
        return getReference(IReminderController.class, "0").unregisterReminder(actor, reminderName);
    }


    public void start()
    {
        List<ClassPath.ResourceInfo> actorClassesRes = ClassPath.get().getAllResources().stream().filter(r -> r.getResourceName().startsWith("META-INF/orbit/actors/classes")).collect(Collectors.toList());

        List<ClassPath.ResourceInfo> actorInterfacesRes = ClassPath.get().getAllResources().stream().filter(r -> r.getResourceName().startsWith("META-INF/orbit/actors/interfaces")).collect(Collectors.toList());
        try
        {
            for (ClassPath.ResourceInfo irs : actorInterfacesRes)
            {
                InterfaceDescriptor descriptor = new InterfaceDescriptor();
                String nameFactoryName = IOUtils.toString(irs.url().openStream());
                descriptor.factory = (ActorFactory<?>) classForName(nameFactoryName).newInstance();
                descriptor.invoker = descriptor.factory.getInvoker();
                descriptor.isObserver = IActorObserver.class.isAssignableFrom(descriptor.factory.getInterface());

                descriptorMapByInterface.put(descriptor.factory.getInterface(), descriptor);
                descriptorMapByInterfaceId.put(descriptor.factory.getInterfaceId(), descriptor);
            }
            List<String> availableActors = new ArrayList<>();
            if (autoDiscovery)
            {
                for (ClassPath.ResourceInfo irs : actorClassesRes)
                {
                    String className = irs.getResourceName().substring("META-INF/orbit/actors/classes".length() + 1);
                    Class<?> actorClass = classForName(className);
                    for (Class<?> interfaceClass : actorClass.getInterfaces())
                    {
                        final InterfaceDescriptor interfaceDescriptor = descriptorMapByInterface.get(interfaceClass);
                        if (interfaceDescriptor != null)
                        {
                            availableActors.add(interfaceDescriptor.factory.getInterface().getName());
                            interfaceDescriptor.concreteClassName = IOUtils.toString(irs.url().openStream());
                            break;
                        }
                    }
                }
            }
            if (actorClasses != null && !actorClassesRes.isEmpty())
            {
                for (Class<?> actorClass : actorClasses)
                {
                    for (Class<?> interfaceClass : actorClass.getInterfaces())
                    {
                        final InterfaceDescriptor interfaceDescriptor = descriptorMapByInterface.get(interfaceClass);
                        if (interfaceDescriptor != null)
                        {
                            availableActors.add(interfaceDescriptor.factory.getInterface().getName());
                            interfaceDescriptor.concreteClassName = actorClass.getName();
                            break;
                        }
                    }
                }
            }
            this.availableActors = Collections.unmodifiableList(availableActors);
        }
        catch (Throwable e)
        {
            throw new UncheckedException(e);
        }
        createObjectReference(IHosting.class, hosting, "");

        if (executor == null)
        {
            executor = ExecutorUtils.newScalingThreadPool(1000);
        }
        executionSerializer = new ExecutionSerializer(executor);
        providers.forEach(v -> v.start());
        // schedules the cleanup
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                ForkJoinTask.adapt(() -> activationCleanup(true)).fork();
            }
        }, cleanupIntervalMillis, cleanupIntervalMillis);

        // TODO move this logic the messaging class
        // schedules the message cleanup
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                messaging.timeoutCleanup();
            }
        }, 5000, 5000);
    }

    private Class<?> classForName(final String className)
    {
        try
        {
            return Class.forName(className);
        }
        catch (Error | Exception ex)
        {
            throw new Error("Error loading class: " + className, ex);
        }
    }

    private ActorFactory findFactoryFor(final Class<?> baseInterface, final Object instance)
    {
        for (Class aInterface : instance.getClass().getInterfaces())
        {
            if (baseInterface.isAssignableFrom(aInterface))
            {
                ActorFactory factory = getDescriptor(aInterface).factory;
                if (factory != null)
                {
                    return factory;
                }
            }
        }
        return null;
    }

    private InterfaceDescriptor getDescriptor(final Class aInterface)
    {
        return descriptorMapByInterface.get(aInterface);
    }

    private InterfaceDescriptor getDescriptor(final int interfaceId)
    {
        return descriptorMapByInterfaceId.get(interfaceId);
    }

    public void onMessageReceived(final INodeAddress from,
                                  final boolean oneway, final int messageId, final int interfaceId, final int methodId,
                                  final Object key, final Object[] params)
    {
        EntryKey entryKey = new EntryKey(interfaceId, key);
        if (logger.isDebugEnabled())
        {
            logger.debug("onMessageReceived for: " + entryKey);
        }
        messagesReceived.incrementAndGet();
        if (!executionSerializer.offerJob(entryKey,
                () -> handleOnMessageReceived(entryKey, from, oneway, messageId, interfaceId, methodId, key, params), maxQueueSize))
        {
            refusedExecutions.incrementAndGet();
            if (logger.isErrorEnabled())
            {
                logger.error("Execution refused: " + key + ":" + interfaceId + ":" + methodId + ":" + messageId);
            }
            if (!oneway)
            {
                messaging.sendResponse(from, 3, messageId, "Execution refused");
            }
        }
    }

    // this method is executed serially by entryKey
    private Task<?> handleOnMessageReceived(final EntryKey entryKey, final INodeAddress from,
                                            final boolean oneway, final int messageId, final int interfaceId,
                                            final int methodId, final Object key,
                                            final Object[] params)
    {
        messagesHandled.incrementAndGet();
        final InterfaceDescriptor descriptor = getDescriptor(interfaceId);
        if (descriptor.isObserver)
        {
            final IActorObserver observer = observerInstances.get(entryKey);
            if (observer == null)
            {
                if (!oneway)
                {
                    messaging.sendResponse(from, 3, messageId, "Observer no longer present");
                }
                return Task.done();
            }
            final Task<?> task = descriptor.invoker.safeInvoke(observer, methodId, params);
            return task.whenComplete((r, e) ->
                    sendResponseAndLogError(oneway, from, messageId, (Object) r, e));
        }

        ReferenceEntry entry = localActors.get(entryKey);


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
            entry.reference = (ActorReference) descriptor.factory.createReference(key != null ? String.valueOf(key) : null);
            entry.reference.runtime = this;
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
            return executeMessage(theEntry, oneway, descriptor, methodId, params, from, messageId);
        }
        else
        {
            if (!executionSerializer.offerJob(null,
                    () -> executeMessage(theEntry, oneway, descriptor, methodId, params, from, messageId),
                    maxQueueSize))
            {
                refusedExecutions.incrementAndGet();
                if (logger.isErrorEnabled())
                {
                    logger.info("Execution refused: " + key + ":" + interfaceId + ":" + methodId + ":" + messageId);
                }
                if (!oneway)
                {
                    messaging.sendResponse(from, 3, messageId, "Execution refused");
                }
            }
            return Task.done();
        }

    }

    private Task<?> executeMessage(
            final ReferenceEntry theEntry,
            final boolean oneway,
            final InterfaceDescriptor descriptor,
            final int methodId,
            final Object[] params,
            final INodeAddress from,
            final int messageId)
    {
        try
        {
            Activation activation = theEntry.popActivation();
            activation.lastAccess = clock.millis();
            Task<?> future;
            try
            {
                Runtime.setRuntime(this.cachedRef);
                future = descriptor.invoker.safeInvoke(activation.getOrCreateInstance(), methodId, params);
                return future.whenComplete((r, e) -> {
                    sendResponseAndLogError(oneway, from, messageId, r, e);
                });
            }
            finally
            {
                // we don't need to unset the Runtime, @see Runtime.setRuntime:
                theEntry.pushActivation(activation);
            }
        }
        catch (Exception ex)
        {
            sendResponseAndLogError(oneway, from, messageId, null, ex);
        }
        return Task.done();
    }

    protected void sendResponseAndLogError(boolean oneway, final INodeAddress from, int messageId, Object result, Throwable exception)
    {
        if (exception != null && logger.isErrorEnabled())
        {
            logger.error("Unknown application error. ", exception);
        }

        if (!oneway)
        {
            try
            {
                if (exception == null)
                {
                    messaging.sendResponse(from, 1, messageId, result);
                }
                else
                {
                    messaging.sendResponse(from, 2, messageId, exception);
                }
            }
            catch (Exception ex2)
            {
                if (logger.isErrorEnabled())
                {
                    logger.error("Error sending method result", ex2);
                }
                try
                {
                    messaging.sendResponse(from, 2, messageId, ex2);
                }
                catch (Exception ex3)
                {
                    if (logger.isErrorEnabled())
                    {
                        logger.error("Failed twice sending result. ", ex2);
                    }
                    try
                    {
                        messaging.sendResponse(from, 3, messageId, "failed twice sending result");
                    }
                    catch (Exception ex4)
                    {
                        logger.error("Failed sending exception. ", ex4);
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unsafe", "unchecked"})
    <T extends IActorObserver> T createReference(final INodeAddress a, final Class<T> iClass, String id)
    {
        final InterfaceDescriptor descriptor = getDescriptor(iClass);
        ActorReference reference = (ActorReference) descriptor.factory.createReference(null);
        reference.address = a;
        reference.runtime = this;
        reference.id = id;
        return (T) reference;
    }

    @SuppressWarnings({"unsafe", "unchecked"})
    public <T extends IActor> T getReference(final Class<T> iClass, final Object id)
    {
        final InterfaceDescriptor descriptor = getDescriptor(iClass);
        ActorReference reference = (ActorReference) descriptor.factory.createReference(id != null ? String.valueOf(id) : null);
        reference.runtime = this;
        return (T) reference;
    }

    public Task sendMessage(IAddressable toReference, boolean oneWay, final int methodId, final Object[] params)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("sending message to " + toReference);
        }
        ActorReference actorReference = (ActorReference) toReference;
        INodeAddress toNode = actorReference.address;
        if (toNode == null)
        {
            return hosting.locateActor(actorReference)
                    .thenCompose(x -> messaging.sendMessage(x, oneWay, actorReference._interfaceId(), methodId, actorReference.id, params));
        }
        return messaging.sendMessage(toNode, oneWay, actorReference._interfaceId(), methodId, actorReference.id, params);
    }

    public void activationCleanup(boolean block)
    {

        long cutOut = clock.millis() - TimeUnit.MINUTES.toMillis(10);
        final List<Task<?>> futures = block ? new ArrayList<>() : null;
        for (Iterator<Map.Entry<EntryKey, ReferenceEntry>> iterator = localActors.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry<EntryKey, ReferenceEntry> mEntry = iterator.next();
            final ReferenceEntry entry = mEntry.getValue();
            if (!entry.removable)
            {
                continue;
            }
            Activation act = entry.peekOldActivation();
            if (act == null)
            {
                continue;
            }
            if (act.lastAccess < cutOut)
            {
                Task task1 = new Task();
                final Supplier<Task<?>> task = () -> {
                    try
                    {
                        final Task<?> res = entry.cleanup(mEntry.getKey(), cutOut);
                        if (res != null && !res.isDone())
                        {
                            res.whenComplete((r, e) -> {
                                if (e != null)
                                {
                                    task1.completeExceptionally(e);
                                }
                                else
                                {
                                    task1.complete(r);
                                }
                            });
                        }
                        else
                        {
                            task1.complete(null);
                        }
                        return res;
                    }
                    catch (Error | RuntimeException ex)
                    {
                        task1.completeExceptionally(ex);
                        throw ex;
                    }
                    catch (Throwable ex)
                    {
                        task1.completeExceptionally(ex);
                        throw new UncheckedException(ex);
                    }
                };
                if (executionSerializer.offerJob(mEntry.getKey(), task, maxQueueSize) && block)
                {
                    futures.add(task1);
                }
            }
        }
        if (block)
        {
            Task.allOf(futures).join();
        }
    }

    public boolean isAutoDiscovery()
    {
        return autoDiscovery;
    }

    public void setAutoDiscovery(final boolean autoDiscovery)
    {
        this.autoDiscovery = autoDiscovery;
    }

    public void addActorClasses(Collection<Class<?>> classes)
    {
        actorClasses.addAll(classes);
    }

    public List<String> getAvailableActors()
    {
        return availableActors;
    }
}
