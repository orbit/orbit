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

import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.LifetimeExtension;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskFunction;
import com.ea.orbit.exception.UncheckedException;

import java.util.Objects;

import static com.ea.orbit.async.Await.await;

public class ActorEntry<T extends AbstractActor> extends ActorBaseEntry<T>
{
    private T actor;
    private Object key;

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
        lastAccess = runtime.clock().millis();
        return executionSerializer.offerJob(key, () -> doRun(function), 1000);
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
        if (actor == null)
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
            if (!Objects.equals(runtime.getLocalAddress(), await(runtime.locateActor(reference, true))))
            {
                return Task.fromValue(null);
            }
        }
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

    /**
     * This must not fail. If errors it should log them instead of throwing
     */
    @Override
    public Task deactivate()
    {
        try
        {
            if (isDeactivated())
            {
                return Task.done();
            }
            return executionSerializer.offerJob(key, () -> doDeactivate(), 1000);
        }
        catch (Throwable ex)
        {
            // this should never happen, but deactivate must't fail.
            ex.printStackTrace();
            return Task.done();
        }
    }

    protected Task<?> doDeactivate()
    {
        if (actor != null)
        {
            try
            {
                await(deactivate(getObject()));
                actor = null;
            }
            catch (Throwable ex)
            {
                try
                {
                    getLogger().error("error deactivating " + getRemoteReference(), ex);
                }
                catch (Throwable ex2)
                {
                    ex2.printStackTrace();
                    ex.printStackTrace();
                }
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
        catch (Throwable ex)
        {
            getLogger().error("Error on actor " + reference + " deactivation", ex);
        }
        await(Task.allOf(runtime.getAllExtensions(LifetimeExtension.class).stream().map(v -> v.postDeactivation(actor))));
        if (key == reference)
        {
            // removing non stateless actor from the distributed directory
            ((Stage) runtime).getHosting().actorDeactivated(reference);
        }
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
}
