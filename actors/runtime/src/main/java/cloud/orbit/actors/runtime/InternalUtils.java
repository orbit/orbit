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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.concurrent.Task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Internal utility class. DO NOT use it outside the orbit project.
 *
 * This class is not part of the public orbit api and its api may change from version to version.
 */
public class InternalUtils
{
    private static final Logger logger = LoggerFactory.getLogger(InternalUtils.class);

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

    /**
     * Invokes Thread.sleep(). If sleep() is interrupted, interrupted-state will be
     * asserted again, in order to support cancellation.
     *
     * For explanation of cancellation in this context, see: http://g.oswego.edu/dl/cpj/cancel.html
     *
     * @param millis
     */
    public static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            // re-assert interrupted-state
            Thread.currentThread().interrupt();
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

    @SuppressWarnings("unchecked")
    public static void linkFuturesOnError(CompletableFuture source, CompletableFuture target)
    {
        if (source != null && (!source.isDone() || source.isCompletedExceptionally()))
        {
            ((CompletableFuture<Object>) source).whenComplete((r, e) -> {
                if (e != null)
                {
                    target.completeExceptionally(e);
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

    private static final char[] HEXES = "0123456789abcdef".toCharArray();

    public static String hexDump(int columns, byte[] raw, int offset, int length)
    {
        int i = offset, j = offset, end = offset + length;

        int x = 0, w = ((length / columns) + 1) * columns;
        final StringBuilder hex = new StringBuilder(w * 4 + columns + 20);
        hex.append("size: ").append(length).append("\r\n");
        for (; x < w; x++)
        {
            if (i < end)
            {
                // while there are chars to read
                final byte ch = raw[i++];
                hex.append(HEXES[(ch & 0xF0) >> 4]).append(HEXES[ch & 0x0F]);
            }
            else
            {
                // complete the rest of the line
                hex.append(' ').append(' ');
            }
            hex.append((x % 8 == 7) ? '|' : ' ');
            if (x % columns == (columns - 1))
            {
                // print char representation of the bytes
                hex.append(' ');
                for (; j < i; j++)
                {
                    final byte ch = raw[j];
                    hex.append(ch >= 32 ? (char) ch : '_');
                }
                hex.append("\r\n");
            }
        }
        return hex.toString();
    }

    public static <R> Task<R> safeInvoke(Supplier<Task<R>> supplier)
    {
        try
        {
            final Task<R> task = supplier.get();
            return task == null ? Task.fromValue(null) : task;
        }
        catch (Throwable ex)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failed invoking task", ex);
            }
            return Task.fromException(ex);
        }
    }
}
