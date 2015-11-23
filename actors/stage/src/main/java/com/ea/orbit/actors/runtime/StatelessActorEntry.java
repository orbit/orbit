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

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentLinkedDeque;

public class StatelessActorEntry<T extends AbstractActor> extends ActorBaseEntry<T>
{
    private ConcurrentLinkedDeque<SoftReference<T>> activations = new ConcurrentLinkedDeque<>();

    public StatelessActorEntry(final RemoteReference reference)
    {
        super(reference);
    }

    @Override
    public T getObject()
    {
        return null;
    }

    @Override
    public Task<?> run(final Function<T, Task<?>> function)
    {
        lastAccess = runtime.clock().millis();
        T actor = tryPop();
        if (actor == null)
        {
            final ActorTaskContext context = ActorTaskContext.pushNew();
            try
            {
                activate().thenAccept(newActor -> executionSerializer.offerJob(newActor, () -> doRun(newActor, function), 1000));
            }
            finally
            {
                context.pop();
            }
        }
        else
        {
            executionSerializer.offerJob(actor, () -> doRun(actor, function), 1000);
        }
        return Task.done();
    }

    @Override
    public boolean isDeactivated()
    {
        return false;
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
            setDeactivated(true);
            activations.clear();
        }
        finally
        {
            completion.complete(null);
        }
        return Task.done();
    }

    T tryPop()
    {
        while (true)
        {
            final SoftReference<T> ref = activations.pollFirst();
            if (ref == null)
            {
                return null;
            }
            T value = ref.get();
            if (value != null)
            {
                return value;
            }
        }
    }

    void push(T value)
    {
        activations.push(new SoftReference<>(value));
    }

    private Task<?> doRun(T actor, final Function<T, Task<?>> function)
    {
        runtime.bind();
        final ActorTaskContext actorTaskContext = ActorTaskContext.pushNew();
        try
        {
            actorTaskContext.setActor(actor);
            final Task<?> apply;
            try
            {
                apply = function.apply(actor);
            }
            catch (Throwable ex)
            {
                push(actor);
                if (actor.logger.isDebugEnabled())
                {
                    actor.logger.error("Error invoking stateless actor " + getRemoteReference(), ex);
                }
                return Task.done();
            }
            if (apply == null || apply.isDone())
            {
                push(actor);
            }
            else
            {
                apply.whenComplete((r, e) -> push(actor));
            }
            return apply;
        }
        finally
        {
            actorTaskContext.pop();
        }
    }
}
