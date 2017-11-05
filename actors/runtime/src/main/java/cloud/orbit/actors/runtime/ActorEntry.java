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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.extensions.ActorConstructionExtension;
import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import static com.ea.async.Async.await;

public class ActorEntry<T extends AbstractActor> extends ActorBaseEntry<T>
{
    private T actor;
    private Object key;
    private WeakHashMap<Registration, Object> timers;
    private Map<StreamSubscriptionHandle, AsyncStream> streamSubscriptions;

    public ActorEntry(final RemoteReference reference)
    {
        super(reference);
        this.key = reference;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject()
    {
        return actor;
    }

    @Override
    public <R> Task<R> run(final TaskFunction<LocalObjects.LocalObjectEntry<T>, R> function)
    {
        return executionSerializer.offerJob(key, () -> doRun(function), 10000);
    }

    @Override
    public void updateLastAccessTime()
    {
        lastAccess = runtime.clock().millis();
    }

    private <R> Task<R> doRun(final TaskFunction<LocalObjects.LocalObjectEntry<T>, R> function)
    {
        runtime.bind();
        final ActorTaskContext actorTaskContext = ActorTaskContext.pushNew();
        try
        {
            // using await makes the actorTaskContext.pop() run in the wrong thread.
            // the the internal par is separated
            return doRunInternal(function, actorTaskContext);
        }
        finally
        {
            actorTaskContext.pop();
        }
    }

    private <R> Task<R> doRunInternal(final TaskFunction<LocalObjects.LocalObjectEntry<T>, R> function, final ActorTaskContext actorTaskContext)
    {
        if (actor == null && !isDeactivated())
        {
            this.actor = await(activate());
            runtime.bind();
        }
        actorTaskContext.setActor(this.getObject());
        return function.apply(this);
    }

    protected Task<T> activate()
    {
        lastAccess = runtime.clock().millis();
        if (key == reference)
        {
            // double checks that this actor really should be activated here.
            if (!Objects.equals(runtime.getLocalAddress(), await(runtime.locateActor(reference, true))))
            {
                return Task.fromValue(null);
            }
        }
        final Object newInstance = runtime.getFirstExtension(ActorConstructionExtension.class).newInstance(concreteClass);
        if (!AbstractActor.class.isInstance(newInstance))
        {
            throw new IllegalArgumentException(String.format("%s is not an actor class", concreteClass));
        }
        final AbstractActor<?> actor = (AbstractActor<?>) newInstance;
        ActorTaskContext.current().setActor(actor);
        actor.reference = reference;
        actor.runtime = runtime;
        actor.stateExtension = storageExtension;
        actor.logger = loggerExtension.getLogger(actor);
        actor.activation = this;

        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.preActivation(actor))));

        if (actor.stateExtension != null)
        {
            try
            {
                await(actor.readState());
            }
            catch (final Exception ex)
            {
                if (actor.logger.isErrorEnabled())
                {
                    actor.logger.error("Error reading actor state for: " + reference, ex);
                }
                throw ex;
            }
        }
        try
        {
            await(actor.activateAsync());
        }
        catch (final Exception ex)
        {
            if (actor.logger.isErrorEnabled())
            {
                actor.logger.error("Error activating actor for: " + reference, ex);
            }
            throw ex;
        }
        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postActivation(actor))));
        return Task.fromValue((T) actor);
    }

    /**
     * This must not fail. If errors it should log them instead of throwing
     */
    @Override
    public Task<Void> deactivate()
    {
        try
        {
            if (isDeactivated())
            {
                return Task.done();
            }
            return executionSerializer.offerJob(key, this::doDeactivate, 10000);
        }
        catch (final Throwable ex)
        {
            // this should never happen, but deactivate must not fail.
            try
            {
                getLogger().error("Error executing action", ex);
            }
            catch (Throwable ex2)
            {
                // just to be on the safe side... loggers can fail...
                ex2.printStackTrace();
                ex.printStackTrace();
            }
            return Task.done();
        }
    }

    protected Task<Void> doDeactivate()
    {
        if (actor != null)
        {
            try
            {
                await(deactivate(getObject()));
            }
            catch (final Throwable ex)
            {
                try
                {
                    getLogger().error("Error deactivating " + getRemoteReference(), ex);
                }
                catch (final Throwable ex2)
                {
                    ex.printStackTrace();
                    ex2.printStackTrace();
                }
            }
            finally
            {
                actor = null;
            }
        }
        setDeactivated(true);
        return Task.done();
    }

    protected Task<Void> deactivate(final T actor)
    {
        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.preDeactivation(actor))));
        try
        {
            await(actor.deactivateAsync());
        }
        catch (final Throwable ex)
        {
            getLogger().error("Error on actor " + reference + " deactivation", ex);
        }
        clearTimers();
        await(clearStreamSubscriptions());
        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postDeactivation(actor))));
        return Task.done();
    }


    public Object getKey()
    {
        return key;
    }

    public void setKey(final Object key)
    {
        this.key = key;
    }

    public synchronized void addTimer(final Registration registration)
    {
        if (timers == null)
        {
            timers = new WeakHashMap<>();
        }
        timers.put(registration, Boolean.TRUE);
    }

    public synchronized void clearTimers()
    {
        if (timers != null)
        {
            timers.keySet().stream()
                    .filter(r -> r != null)
                    .forEach(Registration::dispose);
            timers.clear();
            timers = null;
        }
    }

    public synchronized <T> void addStreamSubscription(final StreamSubscriptionHandle<T> subscription, AsyncStream<T> stream)
    {
        if (streamSubscriptions == null)
        {
            streamSubscriptions = new HashMap<>();
        }
        streamSubscriptions.put(subscription, stream);
    }

    public synchronized <T> void removeStreamSubscription(final StreamSubscriptionHandle<T> subscription, AsyncStream<T> stream)
    {
        if (streamSubscriptions != null)
        {
            streamSubscriptions.remove(subscription, stream);
        }
    }

    public Task<Void> clearStreamSubscriptions()
    {
        if (streamSubscriptions != null)
        {
            final ArrayList<Map.Entry<StreamSubscriptionHandle, AsyncStream>> list;
            synchronized (this)
            {
                list = new ArrayList<>(streamSubscriptions.size());
                list.addAll(streamSubscriptions.entrySet());
                streamSubscriptions.clear();
                streamSubscriptions = null;
            }
            return Task.allOf(list.stream().map(e -> e.getValue().unsubscribe(e.getKey())));
        }
        return Task.done();
    }
}
