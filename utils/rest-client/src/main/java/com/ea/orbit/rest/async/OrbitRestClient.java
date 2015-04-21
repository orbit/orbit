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

package com.ea.orbit.rest.async;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Sperry
 */
public class OrbitRestClient
{
    private WebTarget target;
    private ConcurrentHashMap<Class<?>, Object> proxies = new ConcurrentHashMap<>();

    public OrbitRestClient(WebTarget webTarget)
    {
        target = webTarget;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> interfaceClass)
    {
        Object proxy = proxies.get(interfaceClass);
        if (proxy == null)
        {
            proxy = Proxy.newProxyInstance(OrbitRestClient.class.getClassLoader(), new Class[]{ interfaceClass },
                    (theProxy, method, args) -> invoke(theProxy, method, args));
            proxies.put(interfaceClass, proxy);
        }
        return (T) proxy;
    }

    private Object invoke(final Object proxy, final Method method, final Object[] args)
    {
        CompletableFuture<Object> future = new CompletableFuture<>();
        target.path("/")
                .request().async()
                // the challenge is to create an invocation callback with the proper implementation type
                // solution: asm or javassist
                .get(new InvocationCallback<String>()
                {
                    @Override
                    public void completed(String response)
                    {
                        future.complete(response);
                    }

                    @Override
                    public void failed(Throwable throwable)
                    {
                        future.completeExceptionally(throwable);
                    }
                });
        return future;
    }

}
