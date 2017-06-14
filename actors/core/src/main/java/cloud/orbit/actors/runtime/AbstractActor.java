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

import cloud.orbit.actors.Remindable;
import cloud.orbit.actors.extensions.StorageExtension;
import cloud.orbit.actors.extensions.StreamProvider;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Base class to all actor implementations.
 *
 * @param <T> a class that represents the state of this actor.
 */
public abstract class AbstractActor<T>
{
    protected T state;
    protected StorageExtension stateExtension;
    protected RemoteReference<?> reference;
    protected Logger logger;
    protected ActorRuntime runtime;
    protected Object activation;

    protected AbstractActor()
    {
        this.createDefaultState();
    }

    /**
     * The recommended way to log from an actor.
     *
     * @return this actor's slf4j logger
     */
    protected Logger getLogger()
    {
        return logger != null ? logger : (logger = LoggerFactory.getLogger(getClass()));
    }

    /**
     * The recommended way to log from an actor.
     *
     * @return this actor's slf4j logger
     * @deprecated get the runtime and ask for a logger instead.
     */
    @Deprecated
    protected Logger getLogger(String name)
    {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Creates a default state representation for this actor
     */
    @SuppressWarnings({ "PMD.LooseCoupling", "unchecked" })
    protected void createDefaultState()
    {
        final Class<?> c = getStateClass();
        try
        {
            final Object newState = c.newInstance();
            this.state = (T) newState;
        }
        catch (final Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    protected Class<?> getStateClass()
    {
        final Class<? extends AbstractActor> aClass = getClass();
        return ActorFactoryGenerator.makeStateClass(aClass);
    }

    /**
     * The current actor's persistable state.
     *
     * @return the state
     */
    protected T state()
    {
        return state;
    }

    /**
     * Asynchronously writes the actor's state.
     *
     * @return a completion promise
     */
    protected Task<Void> writeState()
    {
        if (stateExtension == null)
        {
            throw new IllegalStateException("Storage extension not available");
        }
        return stateExtension.writeState(reference, state);
    }

    /**
     * Asynchronously reads the actor's state.
     *
     * @return a completion promise
     */
    protected Task<Boolean> readState()
    {
        if (stateExtension == null)
        {
            throw new IllegalStateException("Storage extension not available");
        }
        return stateExtension.readState(reference, state);
    }

    /**
     * Asynchronously clears the actor's state. This means removing the database entry for this actor.
     *
     * @return a completion promise
     */
    protected Task<Void> clearState()
    {
        if (stateExtension == null)
        {
            throw new IllegalStateException("Storage extension not available");
        }
        return stateExtension.clearState(reference, state).thenRun(this::createDefaultState);
    }

    /**
     * Registers a timer for the current actor. The timer is automatically disposed ondeactivation.
     * The timer calls will not keep the actor active. Timer calls are serialized according to the actor policy.
     * Each stateless actor activation has it's on set of timers.
     *
     * @param futureCallable a callable that returns a Task
     * @param dueTime        Time to the first timer call
     * @param period         Interval between calls, if period <= 0 then the timer will be single shot.
     * @param timeUnit       Time unit for dueTime and period
     * @return A registration object that allows the actor to cancel the timer.
     */
    protected Registration registerTimer(Callable<Task<?>> futureCallable, long dueTime, long period, TimeUnit timeUnit)
    {
        return runtime.registerTimer(this, futureCallable, dueTime, period, timeUnit);
    }

    /**
     * Registers a single shot timer for the current actor. The timer is automatically disposed on deactivation.
     * The timer calls will not keep the actor active. Timer calls are serialized according to the actor policy.
     * Each stateless actor activation has it's on set of timers.
     *
     * @param futureCallable a callable that returns a Task
     * @param dueTime        Time to the first timer call
     * @param timeUnit       Time unit for dueTime and period
     * @return A registration object that allows the actor to cancel the timer.
     */
    protected Registration registerTimer(Callable<Task<?>> futureCallable, long dueTime, TimeUnit timeUnit)
    {
        return registerTimer(futureCallable, dueTime, 0L, timeUnit);
    }

    /**
     * Registers or updated a persisted reminder.
     * Reminders are low frequency persisted timers.
     * They survive the actor's deactivation and even a cluster restart.
     *
     * @param reminderName the remainder's name
     * @return completion promise for this operation
     */
    protected Task<?> registerReminder(String reminderName, long dueTime, long period, TimeUnit timeUnit)
    {
        if (!(this instanceof Remindable))
        {
            throw new IllegalArgumentException("This must implement IRemindable: " + this.getClass().getName());
        }
        return runtime.registerReminder((Remindable) reference, reminderName, dueTime, period, timeUnit);
    }

    /**
     * Removes a previously registered reminder.
     *
     * @param reminderName the remainder's name
     * @return completion promise for this operation
     */
    protected Task<?> unregisterReminder(String reminderName)
    {
        return runtime.unregisterReminder((Remindable) reference, reminderName);
    }

    /**
     * Gets a string that represents uniquely the node that currently holds this actor.
     *
     * @return unique identity string
     */
    protected String runtimeIdentity()
    {
        return runtime.runtimeIdentity();
    }

    /**
     * Gets a string that represents uniquely this actor
     *
     * @return unique identity string
     */
    protected String actorIdentity()
    {
        return reference.id.toString();
    }

    /**
     * Called by the framework when activating the actor.
     *
     * @return a completion promise
     */
    public Task<?> activateAsync()
    {
        return Task.done();
    }

    /**
     * Called by the framework when deactivating the actor.
     *
     * @return a completion promise
     */
    public Task<?> deactivateAsync()
    {
        return Task.done();
    }

    protected StreamProvider getStreamProvider(String name)
    {
        final StreamProvider provider = runtime.getStreamProvider(name);

        // obs.: the actor runtime wraps the StreamProvider
        // to use an actor executor for call backs
        // and to ensure that the actor unsubscribes on deactivation.
        return provider;
    }
}
