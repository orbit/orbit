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

package cloud.orbit.actors.streams.simple;

import cloud.orbit.actors.Addressable;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorRuntime;
import cloud.orbit.actors.util.IdUtils;
import cloud.orbit.concurrent.Task;

import java.util.concurrent.ConcurrentHashMap;

import static com.ea.async.Async.await;

public class SimpleStreamActor extends AbstractActor<SimpleStreamActor.State> implements SimpleStream
{
    public static class State
    {
        ConcurrentHashMap<String, SimpleStreamProxy> subscribers = new ConcurrentHashMap<>();
    }

    @Override
    public Task<Void> unsubscribe(final String handle)
    {
        if (state().subscribers.remove(handle) != null)
        {
            writeState();
        }
        return Task.done();
    }

    @Override
    public Task<String> subscribe(final SimpleStreamProxy subscriber)
    {
        // better than using a count because the count may go backwards if the node fails.
        String handle = IdUtils.urlSafeString(96);
        state().subscribers.put(handle, subscriber);
        writeState();
        return Task.fromValue(handle);
    }

    @Override
    public Task<?> activateAsync()
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("Activating stream: {}", getIdentity());
        }
        await(super.activateAsync());

        int size = state().subscribers.size();

        // check if each subscriber is still alive.
        await(Task.allOf(state().subscribers.entrySet().stream()
                .map(entry -> checkAlive(entry.getKey(), entry.getValue()))));

        if (size != state().subscribers.size())
        {
            return writeState();
        }
        return Task.done();
    }

    @Override
    public Task<?> deactivateAsync()
    {
        await(super.deactivateAsync());

        /*
        int size = state().subscribers.size();

        // check if each subscriber is still alive.
        await(Task.allOf(state().subscribers.entrySet().stream()
                .map(entry -> checkAlive(entry.getKey(), entry.getValue()))));

        if (state().subscribers.isEmpty())
        {
            return clearState();
        }
        else if (size != state().subscribers.size())
        {
            return writeState();
        }
        */

        if (state().subscribers.isEmpty())
        {
            return clearState();
        }

        return Task.done();
    }

    private Task<Boolean> checkAlive(final String handle, final SimpleStreamProxy subscriber)
    {
        final ActorRuntime runtime = ActorRuntime.getRuntime();

        final NodeAddress r = await(runtime.locateActor((Addressable) subscriber, false));
        if (r == null)
        {
            state().subscribers.remove(handle);
            return Task.fromValue(Boolean.FALSE);
        }
        return Task.fromValue(Boolean.TRUE);
    }

    @Override
    public <T> Task<Void> publish(final T data)
    {
        return Task.allOf(state().subscribers.entrySet().stream()
                .map(entry -> entry.getValue().onNext(data, null)
                        .exceptionally(r -> {
                            checkAlive(entry.getKey(), entry.getValue());
                            return null;
                        })));
    }

}
