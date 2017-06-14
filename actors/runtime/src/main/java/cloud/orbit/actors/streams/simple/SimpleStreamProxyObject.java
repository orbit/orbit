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

import cloud.orbit.actors.streams.AsyncObserver;
import cloud.orbit.actors.streams.StreamSequenceToken;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.actors.util.IdUtils;
import cloud.orbit.concurrent.Task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ea.async.Async.await;

public class SimpleStreamProxyObject<T> implements SimpleStreamProxy<T>
{
    private Map<String, AsyncObserver<T>> observerMap = new ConcurrentHashMap<>();
    private volatile Task<String> sharedHandle;
    private final Object mutex = new Object();
    private SimpleStreamExtension provider;
    private SimpleStream streamActorRef;

    public SimpleStreamProxyObject(final SimpleStreamExtension provider, final SimpleStream streamActorRef)
    {
        this.provider = provider;
        this.streamActorRef = streamActorRef;
    }

    @Override
    public Task<Void> onNext(final T data, final StreamSequenceToken sequenceToken)
    {
        await(Task.allOf(observerMap.values().stream()
                .map(v -> {
                    try
                    {
                        return v.onNext(data, null);
                    }
                    catch (Throwable ex)
                    {
                        return Task.<Void>fromException(ex);
                    }
                })));
        return Task.done();
    }

    @Override
    public Task<Void> onError(final Exception ex)
    {
        await(Task.allOf(observerMap.values().stream()
                .map(v -> {
                    try
                    {
                        return v.onError(ex);
                    }
                    catch (Throwable ex2)
                    {
                        return Task.<Void>fromException(ex2);
                    }
                })));
        return Task.done();
    }

    public Task<StreamSubscriptionHandle<T>> subscribe(final AsyncObserver<T> observer)
    {
        String handle = IdUtils.urlSafeString(128);
        observerMap.put(handle, observer);
        synchronized (mutex)
        {
            if (sharedHandle == null)
            {
                sharedHandle = streamActorRef.subscribe(this);
                provider.getHardRefs().add(this);
            }
        }
        await(sharedHandle);
        return Task.fromValue(new SimpleHandle<>(handle));
    }

    public Task<Void> unsubscribe(String handle)
    {
        observerMap.remove(handle);
        if (sharedHandle != null)
        {
            Task<String> sharedHandle = this.sharedHandle;
            final String actualHandle = await(sharedHandle);
            synchronized (mutex)
            {
                if (this.sharedHandle == sharedHandle && observerMap.size() == 0)
                {
                    this.sharedHandle = null;
                    provider.getHardRefs().remove(this);
                    // unsubscribe from the stream
                    return streamActorRef.unsubscribe(actualHandle);
                }
            }
        }
        return Task.done();
    }
}
