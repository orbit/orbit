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
import com.ea.orbit.actors.cache.ExecutionCacheFlushObserver;
import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.cloner.CloneHelper;
import com.ea.orbit.actors.runtime.cloner.ExecutionObjectCloner;
import com.ea.orbit.actors.annotation.CacheResponse;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.tuples.Pair;
import com.ea.orbit.util.AnnotationCache;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

public class ResponseCaching
        extends HandlerAdapter
        implements ExecutionCacheFlushObserver
{
    private static Ticker defaultCacheTicker = null;
    private MessageSerializer messageSerializer;
    private BasicRuntime runtime;
    private final AnnotationCache<CacheResponse> cacheResponseCache = new AnnotationCache<>(CacheResponse.class);

    private static class NullOutputStream extends OutputStream
    {
        @Override
        public void write(int b) throws IOException
        {
        }
    }

    private ExecutionObjectCloner objectCloner;

    /**
     * masterCache is a mapping of caches for each CacheResponse annotated method.
     * The individual caches are defined with a maximum item size and TTL based on the annotation.
     * Key: The method being called (actor agnostic)
     * Value: a cache
     * Key: Pairing of the Actor address, and a hash of the parameters being passed into the method
     * Value: The method result corresponding with the actor's call to the method with provided parameters
     */
    private Cache<Method, Cache<Pair<Addressable, String>, Task>> masterCache = CacheBuilder.newBuilder().build();

    public static void setDefaultCacheTicker(Ticker defaultCacheTicker)
    {
        ResponseCaching.defaultCacheTicker = defaultCacheTicker;
    }

    public ResponseCaching()
    {
    }

    /**
     * Retrieves a cached value for an actor's method.
     * Returns null if there is no cached value.
     */
    public Task<?> get(Method method, Pair<Addressable, String> key)
    {
        Cache<Pair<Addressable, String>, Task> cache = getIfPresent(method);
        return cache != null ? cache.getIfPresent(key) : null;
    }

    /**
     * Caches a value for an actor's method.
     */
    public void put(Method method, Pair<Addressable, String> key, Task<?> value)
    {
        Cache<Pair<Addressable, String>, Task> cache = getCache(method);
        cache.put(key, value);
    }

    private Cache<Pair<Addressable, String>, Task> getIfPresent(Method method)
    {
        CacheResponse cacheResponse = cacheResponseCache.getAnnotation(method);
        if (cacheResponse == null)
        {
            throw new IllegalArgumentException("Passed non-CacheResponse method.");
        }

        return masterCache.getIfPresent(method);
    }

    /**
     * Retrieves the cache for a CacheResponse-annotated method.
     * One is created if necessary.
     */
    private Cache<Pair<Addressable, String>, Task> getCache(Method method)
    {
        CacheResponse cacheResponse = cacheResponseCache.getAnnotation(method);
        if (cacheResponse == null)
        {
            throw new IllegalArgumentException("Passed non-CacheResponse method.");
        }

        Cache<Pair<Addressable, String>, Task> cache = masterCache.getIfPresent(method);
        if (cache == null)
        {
            cache = CacheBuilder.newBuilder()
                    .ticker(defaultCacheTicker == null ? Ticker.systemTicker() : defaultCacheTicker)
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
        RemoteReference actorReference = (RemoteReference) actor;
        Class interfaceClass = RemoteReference.getInterfaceClass(actorReference);

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

    public void setObjectCloner(ExecutionObjectCloner objectCloner)
    {
        this.objectCloner = objectCloner;
    }

    @Override
    public Task<Void> flushWithoutWaiting(Actor actor)
    {
        return flush(actor);
    }

    private Task<?> cacheResponseInvoke(HandlerContext ctx, Invocation invocation)
    {
        String parameterHash = generateParameterHash(invocation.getParams());
        Pair<Addressable, String> key = Pair.of(invocation.getToReference(), parameterHash);

        final Method method = invocation.getMethod();
        Task<?> cached = get(method, key);
        if (cached == null
                || cached.isCompletedExceptionally()
                || cached.isCancelled())
        {
            cached = ctx.write(invocation);
            put(method, key, cached);
        }

        return cached.thenApply((object) -> {
            if (!CloneHelper.needsCloning(object)) {
                return object;
            }
            return objectCloner.clone(object);
        });
    }

    private String generateParameterHash(Object[] params)
    {
        if (params == null || params.length == 0)
        {
            return "";
        }
        try
        {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            DigestOutputStream d = new DigestOutputStream(new NullOutputStream(), md);
            // this entire function shouldn't be here...
            messageSerializer.serializeMessage(runtime, d, new Message().withPayload(params));
            d.close();
            return String.format("%032X", new BigInteger(1, md.digest()));
        }
        catch (Exception e)
        {
            throw new UncheckedException("Unable to make parameter hash", e);
        }
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            final Invocation invocation = (Invocation) msg;
            if (cacheResponseCache.isAnnotated(invocation.getMethod()))
            {
                return cacheResponseInvoke(ctx, invocation);
            }
        }
        return super.write(ctx, msg);
    }

    @Override
    public void onActive(final HandlerContext ctx) throws Exception
    {
        runtime.registerObserver(ExecutionCacheFlushObserver.class, "", this);
        super.onActive(ctx);
    }

    public void setMessageSerializer(MessageSerializer messageSerializer)
    {
        this.messageSerializer = messageSerializer;
    }

    public void setRuntime(BasicRuntime runtime)
    {
        this.runtime = runtime;
    }
}
