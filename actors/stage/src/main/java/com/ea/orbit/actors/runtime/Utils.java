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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

/**
 * Internal utility class. DO NOT use it outside the orbit project.
 */
public class Utils
{
    public static <T> Class<T> classForName(final String className)
    {
        return classForName(className, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> classForName(final String className, boolean ignoreException)
    {
        try
        {
            return (Class<T>) Class.forName(className);
        }
        catch (Error | Exception ex)
        {
            if (!ignoreException)
            {
                throw new Error("Error loading class: " + className, ex);
            }
        }
        return null;
    }

    public static void sleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void linkFutures(CompletableFuture source, CompletableFuture target)
    {
        if (source.isDone() && !source.isCompletedExceptionally())
        {
            target.complete(source.join());
        }
        else
        {
            ((CompletableFuture<Object>) source).whenComplete((r, e) -> {
                if (e != null)
                {
                    target.completeExceptionally(e);
                }
                else
                {
                    target.complete(r);
                }
            });
        }
    }

    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * And return the final value stored in the map.
     *
     * This is equivalent to
     * <pre> {@code
     * if (!map.containsKey(key)) {
     *   map.put(key, value);
     *   return value;
     * } else {
     *   return map.get(key);
     * }}</pre>
     *
     * except that the action is performed atomically.
     */
    public static <K, V> V putIfAbsentAndGet(ConcurrentMap<K, V> map, K key, V newValue)
    {
        final V old = map.putIfAbsent(key, newValue);
        return old != null ? old : newValue;
    }
}
