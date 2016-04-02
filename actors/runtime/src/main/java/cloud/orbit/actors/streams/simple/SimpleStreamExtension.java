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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.actors.extensions.StreamProvider;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.concurrent.ConcurrentHashSet;

public class SimpleStreamExtension implements ActorExtension, StreamProvider
{
    private String name;
    private final Cache<Actor, SimpleStreamProxyObject> weakCache = Caffeine.newBuilder()
            .weakValues()
            .build();

    private final ConcurrentHashSet<SimpleStreamProxyObject> hardRefs = new ConcurrentHashSet<>();


    public SimpleStreamExtension()
    {
    }

    public SimpleStreamExtension(final String name)
    {
        this.name = name;
    }

    @Override
    public <T> AsyncStream<T> getStream(final Class<T> dataClass, final String id)
    {
        return new StreamReference<>(this, dataClass, id);
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public <T> SimpleStreamProxyObject<T> getSubscriber(final SimpleStream streamActorRef)
    {
        //noinspection unchecked
        return weakCache.get(streamActorRef, o -> new SimpleStreamProxyObject<T>(this, streamActorRef));
    }

    ConcurrentHashSet<SimpleStreamProxyObject> getHardRefs()
    {
        return hardRefs;
    }
}
