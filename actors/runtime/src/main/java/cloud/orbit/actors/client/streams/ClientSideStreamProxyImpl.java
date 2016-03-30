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

package cloud.orbit.actors.client.streams;


import cloud.orbit.actors.peer.streams.ClientSideStreamProxy;
import cloud.orbit.actors.peer.streams.ServerSideStreamProxy;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.DefaultClassDictionary;
import cloud.orbit.actors.runtime.InternalUtils;
import cloud.orbit.actors.streams.AsyncObserver;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSequenceToken;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.concurrent.Task;
import cloud.orbit.lifecycle.Startable;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.ea.async.Async.await;

public class ClientSideStreamProxyImpl implements ClientSideStreamProxy, Startable

{
    private ConcurrentMap<StreamKey, ConcurrentMap<Handle, AsyncObserver>> observerMap = new ConcurrentHashMap<>();
    static AtomicLong nextId = new AtomicLong();
    private ClientSideStreamProxy localReference;
    private BasicRuntime runtime;

    @Override
    public Task<Void> onNext(String provider, String streamId, Object message)
    {
        final Map<Handle, AsyncObserver> subscribers = observerMap.get(new StreamKey(provider, message.getClass(), streamId));
        if (subscribers != null)
        {
            subscribers.values().forEach(s -> s.onNext(message, null));
        }
        return Task.done();
    }

    public <T> Task<Void> unsubscribe(final StreamSubscriptionHandle<T> handle)
    {
        ConcurrentMap<Handle, AsyncObserver> observers = observerMap.get(((Handle) handle).key);
        if (observers == null || observers.remove(handle) == null)
        {
            throw new IllegalStateException("not subscribed: " + handle);
        }
        // call server?
        return Task.done();
    }

    @SuppressWarnings("unchecked")
    public <T> Task<StreamSubscriptionHandle<T>> subscribe(String provider, Class<T> dataClass, String streamId, AsyncObserver<T> observer)
    {
        StreamKey streamKey = new StreamKey(provider, dataClass, streamId);
        ConcurrentMap<Handle, AsyncObserver> observers = observerMap.get(streamKey);
        if (observers == null)
        {
            observers = InternalUtils.putIfAbsentAndGet(observerMap, streamKey, new ConcurrentHashMap<>());
            ServerSideStreamProxy serverSideStreamProxy = runtime.getRemoteObserverReference(null, ServerSideStreamProxy.class, "0");
            int dataClassId = DefaultClassDictionary.get().getClassId(dataClass);
            Task<StreamSubscriptionHandle<Object>> subscriptionHandleTask = serverSideStreamProxy.subscribe(provider, dataClassId, streamId, localReference);
            await(subscriptionHandleTask);
        }
        observers = (observers != null) ? observers : InternalUtils.putIfAbsentAndGet(observerMap, streamKey, new ConcurrentHashMap<>());
        Handle handle = new Handle(streamKey, nextId.incrementAndGet());
        observers.put(handle, observer);
        return Task.fromValue(handle);
    }

    public void setRuntime(final BasicRuntime runtime)
    {
        this.runtime = runtime;
    }

    @Override
    public Task<?> start()
    {
        localReference = runtime.registerObserver(ClientSideStreamProxy.class, null, this);
        return Task.done();
    }

    public <T> AsyncStream<T> getStream(final String provider, final Class<T> dataClass, final String id)
    {
        return new AsyncStream<T>()
        {
            @Override
            public Task<Void> unsubscribe(final StreamSubscriptionHandle<T> handle)
            {
                return ClientSideStreamProxyImpl.this.unsubscribe(handle);
            }

            @Override
            public Task<StreamSubscriptionHandle<T>> subscribe(AsyncObserver<T> observer, final StreamSequenceToken token)
            {
                return ClientSideStreamProxyImpl.this.subscribe(provider, dataClass, id, observer);
            }

            @Override
            public Task<Void> publish(final T data)
            {
                return null;
            }
        };
    }

}

class StreamKey
{
    String provider;
    Class dataClass;
    String streamId;

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final StreamKey streamKey = (StreamKey) o;

        if (!provider.equals(streamKey.provider)) return false;
        if (!dataClass.equals(streamKey.dataClass)) return false;
        if (!streamId.equals(streamKey.streamId)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = provider.hashCode();
        result = 31 * result + dataClass.hashCode();
        result = 31 * result + streamId.hashCode();
        return result;
    }

    public StreamKey(final String provider, final Class dataClass, final String streamId)
    {
        this.provider = provider;
        this.dataClass = dataClass;
        this.streamId = streamId;
    }

    @Override
    public String toString()
    {
        return "StreamKey{" +
                "provider='" + provider + '\'' +
                ", dataClass=" + dataClass.getName() +
                ", streamId='" + streamId + '\'' +
                '}';
    }
}


class Handle<T> implements StreamSubscriptionHandle<T>
{
    StreamKey key;
    long id;

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Handle<?> handle = (Handle<?>) o;

        if (id != handle.id) return false;
        return key.equals(handle.key);

    }

    @Override
    public int hashCode()
    {
        int result = key.hashCode();
        result = 31 * result + (int) (id ^ (id >>> 32));
        return result;
    }

    public Handle(final StreamKey key, final long id)
    {
        this.key = key;
        this.id = id;
    }

    @Override
    public String toString()
    {
        return "Handle{" +
                "key=" + key +
                ", id=" + id +
                '}';
    }
}
