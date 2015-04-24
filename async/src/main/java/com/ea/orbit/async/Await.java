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

package com.ea.orbit.async;

import com.ea.orbit.async.instrumentation.InitializeAsync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * In methods annotated with {@literal @}Async calls to <code>await(future)</code>
 * will cause the method to return a CompletableFuture (or Task) instead of blocking.
 * <p/>
 * This is equivalent to use CompletableFuture composition methods (ex: thenApply, handle).
 * The advantage of using <code>await</code> is that the code will resemble sequential blocking code.
 * <p/>
 * Example:
 * <pre><code>
 * import com.ea.orbit.async.Async;
 * import static com.ea.orbit.async.Await.await;
 * ...
 *
 * {@literal@}Async
 * CompletableFuture<Integer> getPageLengthAsync()
 * {
 *     CompletableFuture<String> pageFuture = getPageAsync("http://example.com");
 *     String page = await(pageFuture);
 *     return CompletableFuture.completedFuture(page.length);
 * }</code></pre>
 *
 * Or using orbit Task:
 * <pre><code>
 * {@literal@}Async
 * Task CompletableFuture<Integer> getPageLengthAsync()
 * {
 *     Task<String> pageFuture = getPageAsync("http://example.com");
 *     String page = await(pageFuture);
 *     return Task.fromValue(page.length);
 * }</code></pre>
 *
 * <b>Caveat</b>: The following code must be called before the program execution:
 * {@code static { Await.init() }}
 * Otherwise, the first method to call {@code await()} might be blocking,
 * and a warning message will be printed to the console.
 * Subsequent async methods will work as expected.
 */
public interface Await
{
    static void init()
    {
        InitializeAsync.init();
    }

    Logger logger = LoggerFactory.getLogger(Await.class);

    static <T> T await(CompletableFuture<T> future)
    {
        String warning = "Warning: Illegal call to await, static { Await.init(); } must be added to the main program class and the method must return Task or CompletableFuture";
        if (logger.isDebugEnabled())
        {
            logger.warn(warning, new Throwable());
        }
        else
        {
            logger.warn(warning);
        }
        return future.join();
    }
}
