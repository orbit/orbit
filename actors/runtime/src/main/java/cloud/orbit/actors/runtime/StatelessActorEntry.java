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

import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.ea.async.Async.await;

public class StatelessActorEntry<T extends AbstractActor> extends ActorBaseEntry<T>
{
    private final LocalObjects localObjects;
    private final RemoteReference reference;
    private ConcurrentLinkedDeque<ActorEntry<T>> activations = new ConcurrentLinkedDeque<>();

    public StatelessActorEntry(LocalObjects localObjects, final RemoteReference reference)
    {
        super(reference);
        this.localObjects = localObjects;
        this.reference = reference;
    }

    @Override
    public T getObject()
    {
        return null;
    }

    @Override
    public <R> Task<R> run(final TaskFunction<LocalObjects.LocalObjectEntry<T>, R> function)
    {
        lastAccess = runtime.clock().millis();
        ActorEntry<T> actorEntry = tryPop();
        if (actorEntry == null)
        {
            actorEntry = new ActorEntry<>(reference);
            actorEntry.setExecutionSerializer(executionSerializer);
            actorEntry.setLoggerExtension(loggerExtension);
            actorEntry.setRuntime(runtime);
            actorEntry.setConcreteClass(concreteClass);
            actorEntry.setStorageExtension(storageExtension);
            actorEntry.setKey(actorEntry);

            localObjects.registerEntry(actorEntry, actorEntry);
        }
        final ActorEntry<T> theEntry = actorEntry;
        return actorEntry.run(entry -> doRunInternal(theEntry, entry, function));
    }

    private <R> Task<R> doRunInternal(
            ActorEntry<T> entry1,
            LocalObjects.LocalObjectEntry<T> entry2,
            final TaskFunction<LocalObjects.LocalObjectEntry<T>, R> function)
    {

        try
        {
            Task<R> res = InternalUtils.safeInvoke(() -> function.apply(entry2));
            // the await here is to ensure that the finally block executed at the right time.
            await(res);
            return res;
        }
        finally
        {
            if (isDeactivated() || entry1 != entry2)
            {
                await(entry1.deactivate());
            }
            else
            {
                push(entry1);
            }
        }
    }

    public Task<Void> deactivate()
    {
        List<Task<Void>> tasks = new ArrayList<>();
        for (ActorEntry<T> actorEntry; null != (actorEntry = tryPop()); )
        {
            tasks.add(actorEntry.deactivate());
        }
        await(Task.allOf(tasks));
        setDeactivated(true);
        return Task.done();
    }


    ActorEntry<T> tryPop()
    {
        while (true)
        {
            final ActorEntry<T> actorEntry = activations.pollFirst();
            if (actorEntry == null)
            {
                return null;
            }
            // ignore deactivated entries
            if (!actorEntry.isDeactivated())
            {
                return actorEntry;
            }
        }
    }

    void push(ActorEntry<T> value)
    {
        activations.push(value);
    }

}
