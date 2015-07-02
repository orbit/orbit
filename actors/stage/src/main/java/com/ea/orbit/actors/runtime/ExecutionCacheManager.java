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

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Addressable;
import com.ea.orbit.annotation.CacheResponse;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.tuples.Pair;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionCacheManager implements ExecutionCacheFlushObserver
{
    static private Ticker DefaultCacheTicker = null;

    /**
     * masterCache is a mapping of caches for each CacheResponse annotated method.
     * The individual caches are defined with a maximum item size and TTL based on the annotation.
     * Key: The method being called (actor agnostic)
     * Value: a cache
     *      Key: Pairing of the Actor address, and a hash of the parameters being passed into the method
     *      Value: The method result corresponding with the actor's call to the method with provided parameters
     */
    private Cache<Method, Cache<Pair<Addressable, String>, Task>> masterCache = CacheBuilder.newBuilder().build();

    public static void setDefaultCacheTicker(Ticker defaultCacheTicker)
    {
        DefaultCacheTicker = defaultCacheTicker;
    }

    public ExecutionCacheManager()
    {
    }

    /**
     * Retrieves a cached value for an actor's method.
     * Returns null if there is no cached value.
     */
    public Task<?> get(Method method, Pair<Addressable, String> key)
    {
        Cache<Pair<Addressable, String>, Task> cache = getCache(method);
        return cache.getIfPresent(key);
    }

    /**
     * Caches a value for an actor's method.
     */
    public void put(Method method, Pair<Addressable, String> key, Task<?> value)
    {
        Cache<Pair<Addressable, String>, Task> cache = getCache(method);
        cache.put(key, value);
    }

    /**
     * Retrieves the cache for a CacheResponse-annotated method.
     * One is created if necessary.
     */
    private Cache<Pair<Addressable, String>, Task> getCache(Method method)
    {
        CacheResponse cacheResponse = method.getAnnotation(CacheResponse.class);
        if (cacheResponse == null)
        {
            throw new IllegalArgumentException("Passed non-CacheResponse method.");
        }

        Cache<Pair<Addressable, String>, Task> cache = masterCache.getIfPresent(method);
        if (cache == null)
        {
            cache = CacheBuilder.newBuilder()
                    .ticker(DefaultCacheTicker == null ? Ticker.systemTicker() : DefaultCacheTicker)
                    .maximumSize(cacheResponse.maxEntries())
                    .expireAfterWrite(cacheResponse.ttlDuration(), cacheResponse.ttlUnit())
                    .build();

            masterCache.put(method, cache);
        }

        return cache;
    }

    @Override
    public Task<Void> flush(Actor actor)
    {
        ActorReference actorReference = (ActorReference) actor;
        Class interfaceClass = ActorReference.getInterfaceClass(actorReference);

        masterCache.asMap().entrySet().stream()
                .filter(e -> interfaceClass.equals(e.getKey().getDeclaringClass()))
                .map(e -> e.getValue())
                .forEach(cache -> {
                    List<Pair<Addressable, String>> keys = cache.asMap().keySet().stream()
                            .filter(cacheKey -> cacheKey.getLeft().equals(actorReference))
                            .collect(Collectors.toList());

                    cache.invalidateAll(keys);
                });

        return Task.done();
    }

    @Override
    public Task<Void> flushWithoutWaiting(Actor actor)
    {
        return flush(actor);
    }
}
