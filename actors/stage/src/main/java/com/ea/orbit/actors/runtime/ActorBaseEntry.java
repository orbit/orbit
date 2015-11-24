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

import com.ea.orbit.actors.concurrent.MultiExecutionSerializer;
import com.ea.orbit.actors.extensions.LifetimeExtension;
import com.ea.orbit.actors.extensions.LoggerExtension;
import com.ea.orbit.actors.extensions.StorageExtension;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskFunction;
import com.ea.orbit.exception.NotImplementedException;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;

import static com.ea.orbit.async.Await.await;

public abstract class ActorBaseEntry<T extends AbstractActor> implements LocalObjects.LocalObjectEntry<T>
{
    private final RemoteReference<T> reference;
    Class<T> concreteClass;
    protected ActorRuntime runtime;
    MultiExecutionSerializer<Object> executionSerializer;
    private LoggerExtension loggerExtension;
    private StorageExtension storageExtension;
    private boolean deactivated;
    private Logger logger;
    protected long lastAccess;

    public ActorBaseEntry(final RemoteReference reference)
    {
        this.reference = reference;
    }

    @Override
    public RemoteReference<T> getRemoteReference()
    {
        return reference;
    }

    @Override
    public Task<?> run(final TaskFunction<T, ?> function)
    {
        throw new NotImplementedException();
    }


    protected Task<T> activate()
    {
        lastAccess = runtime.clock().millis();
        Object newInstance;
        try
        {
            newInstance = concreteClass.newInstance();
        }
        catch (Exception ex)
        {
            getLogger().error("Error creating instance of " + concreteClass, ex);
            throw new UncheckedException(ex);
        }
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

        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.preActivation(actor))));

        if (actor.stateExtension != null)
        {
            try
            {
                await(actor.readState());
            }
            catch (Exception ex)
            {
                if (actor.logger.isErrorEnabled())
                {
                    actor.logger.error("Error reading actor state for: " + reference, ex);
                }
                throw ex;
            }
        }
        await(actor.activateAsync());
        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postActivation(actor))));
        return Task.fromValue((T) actor);
    }

    protected Logger getLogger()
    {
        return logger != null ? logger : (logger = loggerExtension.getLogger(reference._interfaceClass()));
    }

    public void setConcreteClass(final Class<T> concreteClass)
    {
        this.concreteClass = concreteClass;
    }

    public void setRuntime(final ActorRuntime runtime)
    {
        this.runtime = runtime;
        this.lastAccess = runtime.clock().millis();
    }

    public void setExecutionSerializer(final MultiExecutionSerializer<Object> executionSerializer)
    {
        this.executionSerializer = executionSerializer;
    }

    public void setStorageExtension(final StorageExtension storageExtension)
    {
        this.storageExtension = storageExtension;
    }

    public void setLoggerExtension(final LoggerExtension loggerExtension)
    {
        this.loggerExtension = loggerExtension;
    }

    public boolean isDeactivated()
    {
        return deactivated;
    }

    /**
     * This must not fail. If errors it should log them instead of throwing
     */
    public abstract Task deactivate();

    protected Task<Void> deactivate(final T actor)
    {
        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.preDeactivation(actor))));
        try
        {
            await(actor.deactivateAsync());
        }
        catch (Throwable ex)
        {
            getLogger().error("Error on actor " + reference + " deactivation", ex);
        }
        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postDeactivation(actor))));
        return Task.done();
    }

    protected void setDeactivated(final boolean deactivated)
    {
        this.deactivated = deactivated;
    }

    public long getLastAccess()
    {
        return lastAccess;
    }
}
