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

import com.ea.orbit.actors.IRemindable;
import com.ea.orbit.actors.providers.*;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.gentyref.GenericTypeReflector;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Base class to all actor implementations.
 *
 * @param <T> a class that represents the state of this actor.
 */
public class OrbitActor<T>
{
    T state;
    IStorageProvider stateProvider;
    ActorReference reference;
    Logger logger;

    @SuppressWarnings({"PMD.LooseCoupling", "unchecked"})
    protected OrbitActor()
    {
        Class c = (Class) GenericTypeReflector.getTypeParameter(getClass(),
                OrbitActor.class.getTypeParameters()[0]);
        if (c == null)
        {
            c = LinkedHashMap.class;
        }
        try
        {
            state = (T) c.newInstance();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    protected Logger getLogger()
    {
        // TODO wrap the logger to add some context about the actor and the current call
        return logger != null ? logger : (logger = LoggerFactory.getLogger(getClass()));
    }

    protected Logger getLogger(String name)
    {
        // TODO wrap the logger to add some context about the actor and the current call
        return LoggerFactory.getLogger(name);
    }

    protected T state()
    {
        return state;
    }

    protected Task<Void> writeState()
    {
        return stateProvider.writeState(reference, state);
    }

    protected Task<Boolean> readState()
    {
        return stateProvider.readState(reference, state);
    }

    protected Task<Void> clearState()
    {
        return stateProvider.clearState(reference, state);
    }

    /**
     * Registers a timer for the current actor. The timer disappears on deactivation.
     * The timer calls do not keep the actor active.
     *
     * @param futureCallable a callable that returns a Task
     * @param dueTime        Time to the first timer call
     * @param period         Interval between calls
     * @param timeUnit       Time unit for dueTime and period
     * @return A registration object that allows the actor to cancel the timer.
     */
    protected Registration registerTimer(Callable<Task<?>> futureCallable, long dueTime, long period, TimeUnit timeUnit)
    {
        return reference.runtime.registerTimer(this, futureCallable, dueTime, period, timeUnit);
    }

    protected Task registerReminder(String name, long dueTime, long period, TimeUnit timeUnit)
    {
        if (!(this instanceof IRemindable))
        {
            throw new IllegalArgumentException("This must implement IRemindable: " + this.getClass().getName());
        }
        return reference.runtime.registerReminder((IRemindable) reference, name, dueTime, period, timeUnit);
    }

    protected Task unregisterReminder(String name)
    {
        return reference.runtime.unregisterReminder((IRemindable)reference, name);
    }

    protected String runtimeIdentity()
    {
        // TODO: return the node address
        return reference.runtime.toString();
    }

    public Task<?> activateAsync()
    {
        return Task.done();
    }

    public Task deactivateAsync()
    {
        return Task.done();
    }
}
