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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Addressable;
import cloud.orbit.actors.annotation.CacheResponse;
import cloud.orbit.actors.cache.ExecutionCacheFlushObserver;
import cloud.orbit.actors.cloner.CloneHelper;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.extensions.ResponseCachingExtension;
import cloud.orbit.actors.net.HandlerAdapter;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.concurrent.MessageDigestFactory;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import cloud.orbit.tuples.Pair;
import cloud.orbit.util.AnnotationCache;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class DefaultResponseCachingExtension
        extends HandlerAdapter
        implements ResponseCachingExtension
{
    private static Clock clock = null;
    private static Executor cacheExecutor = null;
    private MessageSerializer messageSerializer;
    private BasicRuntime runtime;
    private final AnnotationCache<CacheResponse> cacheResponseCache = new AnnotationCache<>(CacheResponse.class);
    private final MessageDigestFactory messageDigest = new MessageDigestFactory("SHA-256");

    private ExecutionObjectCloner objectCloner;

    /**
     * masterCache is a mapping of caches for each CacheResponse annotated method.
     * The individual caches are defined with a maximum item size and TTL based on the annotation.
     * Key: The method being called (actor agnostic)
     * Value: a cache
     * Key: Pairing of the Actor address, and a hash of the parameters being passed into the method
     * Value: The method result corresponding with the actor's call to the method with provided parameters
     */
    private final Cache<Method, Cache<Pair<Addressable, String>, Task>> masterCache = Caffeine.newBuilder().build();

    public static void setCacheExecutor(final Executor cacheExecutor)
    {
        DefaultResponseCachingExtension.cacheExecutor = cacheExecutor;
    }

    public static void setClock(final Clock clock)
    {
        DefaultResponseCachingExtension.clock = clock;
    }

    /**
     * Retrieves a cached value for an actor's method.
     * Returns null if there is no cached value.
     */
    @Override
    public Task<?> get(Method method, Pair<Addressable, String> key)
    {
        final Cache<Pair<Addressable, String>, Task> cache = getIfPresent(method);
        return cache != null ? cache.getIfPresent(key) : null;
    }

    /**
     * Caches a value for an actor's method.
     */
    @Override
    public void put(Method method, Pair<Addressable, String> key, Task<?> value)
    {
        final Cache<Pair<Addressable, String>, Task> cache = getCache(method);
        cache.put(key, value);
    }

    private Cache<Pair<Addressable, String>, Task> getIfPresent(Method method)
    {
        final CacheResponse cacheResponse = cacheResponseCache.getAnnotation(method);
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
        final CacheResponse cacheResponse = cacheResponseCache.getAnnotation(method);
        if (cacheResponse == null)
        {
            throw new IllegalArgumentException("Passed non-CacheResponse method.");
        }

        // if cached, return; otherwise create, cache and return
        return masterCache.get(method, key -> {
            Caffeine<Object, Object> builder = Caffeine.newBuilder();
            if (cacheExecutor != null)
            {
                builder.executor(cacheExecutor);
            }
            return builder
                    .ticker(clock == null ? Ticker.systemTicker() : () -> TimeUnit.MILLISECONDS.toNanos(clock.millis()))
                    .maximumSize(cacheResponse.maxEntries())
                    .expireAfterWrite(cacheResponse.ttlDuration(), cacheResponse.ttlUnit())
                    .build();
        });
    }

    @Override
    public Task<Void> flush(Actor actor)
    {
        final RemoteReference actorReference = (RemoteReference) actor;
        final Class interfaceClass = RemoteReference.getInterfaceClass(actorReference);

        masterCache.asMap().entrySet().forEach(entry -> {
            if (interfaceClass.equals(entry.getKey().getDeclaringClass()))
            {
                entry.getValue().asMap().keySet().removeIf(addressablePair -> addressablePair.getLeft().equals(actorReference));
            }
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
        final String parameterHash = generateParameterHash(invocation.getParams());
        final Pair<Addressable, String> key = Pair.of(invocation.getToReference(), parameterHash);

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
            final MessageDigest md = messageDigest.newDigest();
            final byte[] hashValue = md.digest(messageSerializer.serializeMessage(runtime, new Message().withPayload(params)));
            return String.format("%032X", new BigInteger(1, hashValue));
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
