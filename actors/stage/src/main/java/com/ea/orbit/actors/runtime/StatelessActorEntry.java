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
import com.ea.orbit.concurrent.TaskFunction;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.ea.orbit.async.Await.await;

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
    public <R> Task<R> run(final TaskFunction<T, R> function)
    {
        lastAccess = runtime.clock().millis();
        T actor = tryPop();
        if (actor == null)
        {
            final ActorTaskContext context = ActorTaskContext.pushNew();
            try
            {
                // it's ok to call activate here since no one else can access this actor instance.
                return activate().thenCompose(newActor ->
                        executionSerializer.offerJob(newActor, () -> doRun(newActor, function), 1000));
            }
            finally
            {
                context.pop();
            }
        }
        else
        {
            return executionSerializer.offerJob(actor, () -> doRun(actor, function), 1000);
        }
    }

    @Override
    public boolean isDeactivated()
    {
        return false;
    }

    protected Task<?> doDeactivate()
    {
        // not calling deactivation for stateless
        setDeactivated(true);
        activations.clear();
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

    private <R> Task<R> doRun(T actor, final TaskFunction<T, R> function)
    {
        runtime.bind();
        final ActorTaskContext actorTaskContext = ActorTaskContext.pushNew();
        try
        {
            actorTaskContext.setActor(actor);
            // separating to ensure actorTaskContext.pop() happens in the same thread
            return doRunInternal(actor, function);
        }
        finally
        {
            actorTaskContext.pop();
        }
    }

    private <R> Task<R> doRunInternal(final T actor, final TaskFunction<T, R> function)
    {
        try
        {
            Task<R> res = InternalUtils.safeInvoke(() -> function.apply(actor));
            await(res);
            return res;
        }
        finally
        {
            push(actor);
        }
    }
}
