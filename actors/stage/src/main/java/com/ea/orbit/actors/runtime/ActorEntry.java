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

import com.ea.orbit.concurrent.Task;

import com.google.common.base.Function;

import static com.ea.orbit.async.Await.await;

public class ActorEntry<T extends AbstractActor> extends ActorBaseEntry<T>
{
    private AbstractActor actor;

    public ActorEntry(final RemoteReference reference)
    {
        super(reference);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject()
    {
        return (T) actor;
    }

    @Override
    public Task<?> run(final Function<T, Task<?>> function)
    {
        lastAccess = runtime.clock().millis();
        executionSerializer.offerJob(getRemoteReference(), () -> doRun(function), 1000);
        return Task.done();
    }

    @Override
    public Task deactivate()
    {
        try
        {
            if (isDeactivated())
            {
                return Task.done();
            }
            Task completion = new Task();
            if (!executionSerializer.offerJob(getRemoteReference(), () -> doDeactivate(completion), 1000))
            {
                completion.complete(null);
                getLogger().error("Execution serializer refused task to deactivate instance of " + getRemoteReference());
            }
            return completion;
        }
        catch (Throwable ex)
        {
            // this should never happen, but deactivate must't fail.
            ex.printStackTrace();
            return Task.done();
        }
    }

    private Task<?> doDeactivate(final Task completion)
    {
        try
        {
            if (actor != null)
            {
                try
                {
                    await(super.deactivate(getObject()));
                    actor = null;
                }
                catch (Throwable ex)
                {
                    getLogger().error("error deactivating " + getRemoteReference(), ex);
                }
            }
        }
        catch (Throwable ex)
        {
            // ignore
        }
        setDeactivated(true);
        completion.complete(null);
        return Task.done();
    }

    private Task<?> doRun(final Function<T, Task<?>> function)
    {
        runtime.bind();
        final ActorTaskContext actorTaskContext = ActorTaskContext.pushNew();
        try
        {
            if (actor == null)
            {
                return activate().thenAccept(actor -> {
                    this.actor = actor;
                    runtime.bind();
                    //noinspection ConstantConditions
                    ActorTaskContext.current().setActor(actor);
                    function.apply(getObject());
                });
            }
            else
            {
                actorTaskContext.setActor(getObject());
                return function.apply(getObject());
            }
        }
        finally
        {
            actorTaskContext.pop();
        }
    }

}
