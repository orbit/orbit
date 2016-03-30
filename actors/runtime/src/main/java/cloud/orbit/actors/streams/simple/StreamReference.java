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

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.runtime.DefaultClassDictionary;
import cloud.orbit.actors.streams.AsyncObserver;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSequenceToken;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.concurrent.Task;

import java.io.Serializable;

public class StreamReference<T> implements AsyncStream<T>
{
    private final SimpleStreamExtension provider;
    private final Class<T> dataClass;
    private final SimpleStream streamActorRef;
    SimpleStreamProxyObject<T> subscriberObject;

    public static class SimpleStreamHandle<T> implements StreamSubscriptionHandle<T>, Serializable
    {
        private String id;

        public SimpleStreamHandle()
        {
        }

        public SimpleStreamHandle(final String id)
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }
    }


    public StreamReference(SimpleStreamExtension provider, final Class<T> dataClass, final String id)
    {
        this.provider = provider;
        this.dataClass = dataClass;
        final int dataClassId = DefaultClassDictionary.get().getClassId(dataClass);
        final String oid = dataClassId + ":" + id;

        //noinspection unchecked
        this.streamActorRef = (SimpleStream) Actor.getReference(SimpleStream.class, oid);
    }

    @Override
    public Task<Void> unsubscribe(final StreamSubscriptionHandle<T> handle)
    {
        SimpleStreamProxyObject<Object> subscriber = provider.getSubscriber(streamActorRef);
        return subscriber.unsubscribe(((SimpleHandle<T>) handle).getId());
    }

    @Override
    public Task<StreamSubscriptionHandle<T>> subscribe(final AsyncObserver<T> observer, final StreamSequenceToken token)
    {

        SimpleStreamProxyObject<T> subscriber = provider.getSubscriber(streamActorRef);
        return subscriber.subscribe(observer);
    }

    @Override
    public Task<Void> publish(final T data)
    {
        return streamActorRef.publish(data);
    }

}
