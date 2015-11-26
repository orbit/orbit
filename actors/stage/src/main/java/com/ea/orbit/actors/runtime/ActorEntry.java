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

import static com.ea.orbit.async.Await.await;

public class ActorEntry<T extends AbstractActor> extends ActorBaseEntry<T>
{
    private T actor;

    public ActorEntry(final RemoteReference reference)
    {
        super(reference);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject()
    {
        return actor;
    }

    @Override
    public <R> Task<R> run(final TaskFunction<T, R> function)
    {
        lastAccess = runtime.clock().millis();
        return executionSerializer.offerJob(getRemoteReference(), () -> doRun(function), 1000);
    }

    protected Task<?> doDeactivate()
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

    private <R> Task<R> doRun(final TaskFunction<T, R> function)
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

    private <R> Task<R> doRunInternal(final TaskFunction<T, R> function, final ActorTaskContext actorTaskContext)
    {
        if (actor == null)
        {
            this.actor = await(activate());
            runtime.bind();
        }
        actorTaskContext.setActor(getObject());
        return function.apply(actor);
    }

}
