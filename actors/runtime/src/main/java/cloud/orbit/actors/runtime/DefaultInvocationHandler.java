/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.extensions.InvocationHandlerExtension;
import cloud.orbit.concurrent.Task;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ea.async.Async.await;

/**
 * Adds support for {@link InvocationHandlerExtension}.
 */
public class DefaultInvocationHandler extends SimpleInvocationHandler
{
    private final ConcurrentMap<String, List<InvocationHandlerExtension>> handlerExtensionCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    protected Task<Object> doInvoke(final Stage runtime, final Invocation invocation, final LocalObjects.LocalObjectEntry entry, final LocalObjects.LocalObjectEntry target, final Method method, final Boolean reentrant, final ObjectInvoker invoker)
    {
        final long startTimeNanos = System.nanoTime();
        final List<InvocationHandlerExtension> extensions = handlerExtensionCache.get(runtime.runtimeIdentity());

        final List<InvocationHandlerExtension> invocationHandlerExtensions = extensions == null ?
                handlerExtensionCache.computeIfAbsent(runtime.runtimeIdentity(), s -> Collections.unmodifiableList(runtime.getAllExtensions(InvocationHandlerExtension.class))) :
                extensions;

        // Before invoke actions
        Task<?> beforeInvokeChain = Task.done();
        for (InvocationHandlerExtension invocationHandlerExtension : invocationHandlerExtensions)
        {
            beforeInvokeChain = beforeInvokeChain.thenCompose(() -> invocationHandlerExtension.beforeInvoke(startTimeNanos, target.getObject(), method, invocation.getParams(), invocation.getHeaders()));
        }
        await(beforeInvokeChain);
        beforeInvoke(invocation, method);

        // Real invoke
        final Task<Object> invokeResult = invoker.safeInvoke(target.getObject(), invocation.getMethodId(), invocation.getParams());

        // After invoke actions
        afterInvoke(startTimeNanos, invocation, method);
        Task<?> afterInvokeChain = Task.done();
        for (InvocationHandlerExtension invocationHandlerExtension : invocationHandlerExtensions)
        {
            afterInvokeChain = afterInvokeChain.thenCompose(() -> invocationHandlerExtension.afterInvoke(startTimeNanos, target.getObject(), method, invocation.getParams(), invocation.getHeaders()));
        }
        await(afterInvokeChain);

        // After complete invoke chain actions
        return invokeResult.thenCompose((o) ->
        {
            taskComplete(startTimeNanos, invocation, method);

            Task<?> afterCompleteChain = Task.done();
            for (InvocationHandlerExtension invocationHandlerExtension : invocationHandlerExtensions)
            {
                afterCompleteChain = afterCompleteChain.thenCompose(() -> invocationHandlerExtension.afterInvokeChain(startTimeNanos, target.getObject(), method, invocation.getParams(), invocation.getHeaders()));
            }

            return afterCompleteChain.thenApply((f) -> o);
        });
    }
}
