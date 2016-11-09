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

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.Reentrant;
import cloud.orbit.concurrent.Task;
import cloud.orbit.util.AnnotationCache;

import java.lang.reflect.Method;
import java.util.Map;

public class InvocationHandler
{
    private Logger logger = LoggerFactory.getLogger(InvocationHandler.class);

    private boolean myResult;
    private Task result;

    boolean is()
    {
        return myResult;
    }

    public Task getResult()
    {
        return result;
    }

    public void beforeInvoke(Invocation invocation, Method method)
    {
        logger.debug("Invoking: {}.{}", invocation.getToReference().toString(), method.getName());
    }

    public void afterInvoke(long startTimeMs, Invocation invocation, Method method)
    {

    }

    public void taskComplete(long startTimeMs, Invocation invocation, Method method)
    {

    }

    @SuppressWarnings("unchecked")
    public InvocationHandler invoke(Stage runtime, AnnotationCache<Reentrant> reentrantCache, Invocation invocation, LocalObjects.LocalObjectEntry entry, LocalObjects.LocalObjectEntry target, ObjectInvoker invoker)
    {
        boolean reentrant = false;

        final Method method;
        final long start;

        final ActorTaskContext context = ActorTaskContext.current();
        if (context != null)
        {
            if (invocation.getHeaders() != null && invocation.getHeaders().size() > 0 && runtime.getStickyHeaders() != null)
            {
                invocation.getHeaders().entrySet().forEach(e ->
                {
                    if (runtime.getStickyHeaders().contains(e.getKey()))
                    {
                        context.setProperty(String.valueOf(e.getKey()), e.getValue());
                    }
                });
            }

            method = invoker.getMethod(invocation.getMethodId());

            if (reentrantCache.isAnnotated(method))
            {
                reentrant = true;
                context.setDefaultExecutor(r -> entry.run(o ->
                {
                    r.run();
                    return Task.done();
                }));
            }
            context.setRuntime(runtime);
        }
        else
        {
            method = null;
            runtime.bind();
        }

        if (method != null)
        {
            beforeInvoke(invocation, method);
        }

        start = System.nanoTime();
        result = invoker.safeInvoke(target.getObject(), invocation.getMethodId(), invocation.getParams());

        if (method != null)
        {
            afterInvoke(start, invocation, method);
        }

        if (invocation.getCompletion() != null)
        {
            InternalUtils.linkFutures(result, invocation.getCompletion());
        }

        if (method != null)
        {
            result.thenAccept(n -> taskComplete(start, invocation, method));
        }

        if (reentrant)
        {
            // let the execution serializer proceed if actor method blocks on a task
            myResult = true;
            return this;
        }

        myResult = false;
        return this;
    }
}
