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

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.Reentrant;
import cloud.orbit.actors.annotation.SkipUpdateLastAccess;
import cloud.orbit.actors.extensions.InvocationHandlerExtension;
import cloud.orbit.concurrent.Task;
import cloud.orbit.util.AnnotationCache;

import java.lang.reflect.Method;

/**
 * Simple {@link InvocationHandler} with no support for {@link InvocationHandlerExtension}.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class SimpleInvocationHandler implements InvocationHandler
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AnnotationCache<Reentrant> reentrantCache = new AnnotationCache<>(Reentrant.class);
    private final AnnotationCache<SkipUpdateLastAccess> skipUpdateLastAccessAnnotationCache = new AnnotationCache<>(SkipUpdateLastAccess.class);

    private boolean performanceLoggingEnabled = true;
    private double slowInvokeThresholdMs = 250;
    private double slowTaskThresholdMs = 1000;

    @Override
    public Task<Object> invoke(final Stage runtime, final Invocation invocation, final LocalObjects.LocalObjectEntry entry, final LocalObjects.LocalObjectEntry target, final ObjectInvoker invoker)
    {
        runtime.bind();

        final Method method = invoker.getMethod(invocation.getMethodId());
        final boolean reentrant = reentrantCache.isAnnotated(method);

        if (!skipUpdateLastAccessAnnotationCache.isAnnotated(method)) {
            // when stateless, entry refers to the stateless actor entry and target is the actorentry
            target.updateLastAccessTime();
        }

        final ActorTaskContext context = ActorTaskContext.current();
        if (context != null)
        {
            if (invocation.getHeaders() != null && invocation.getHeaders().size() > 0 && runtime.getStickyHeaders() != null)
            {
                invocation.getHeaders().forEach((key, value) ->
                {
                    if (runtime.getStickyHeaders().contains(key))
                    {
                        context.setProperty(key, value);
                    }
                });
            }

            if (reentrant)
            {
                context.setDefaultExecutor(r -> entry.run(o ->
                {
                    r.run();
                    return Task.done();
                }));
            }
            context.setRuntime(runtime);
        }

        // Perform the internal invocation
        final Task<Object> invokeResult = doInvoke(runtime, invocation, entry, target, method, reentrant, invoker);

        // Link the result to the completion promise
        if (invocation.getCompletion() != null)
        {
            InternalUtils.linkFutures(invokeResult, invocation.getCompletion());
        }

        // If reentrant we say we are done
        if (reentrant)
        {
            return Task.fromValue(null);
        }

        return invokeResult;
    }

    @SuppressWarnings("unchecked")
    protected Task<Object> doInvoke(final Stage runtime, final Invocation invocation, final LocalObjects.LocalObjectEntry entry, final LocalObjects.LocalObjectEntry target, final Method method, final Boolean reentrant, final ObjectInvoker invoker)
    {
        final long startTimeNanos = System.nanoTime();

        beforeInvoke(invocation, method);

        final Task<Object> invokeResult = invoker.safeInvoke(target.getObject(), invocation.getMethodId(), invocation.getParams());

        afterInvoke(startTimeNanos, invocation, method);

        invokeResult.whenComplete((o, throwable) -> taskComplete(startTimeNanos, invocation, method));

        return invokeResult;
    }

    protected void beforeInvoke(Invocation invocation, @Nullable Method method)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Invoking: {}.{}", invocation.getToReference().toString(), method != null ? method.getName() : invocation.getMethodId());
        }
    }

    protected void afterInvoke(long startTimeNanos, Invocation invocation, @Nullable Method method)
    {
        if (performanceLoggingEnabled && logger.isWarnEnabled())
        {
            final long durationNanos = (System.nanoTime() - startTimeNanos);
            final double durationMs = durationNanos / 1_000_000.0;
            if (durationMs > slowInvokeThresholdMs)
            {
                logger.warn("Slow task: {}. {} in {} ms",
                        invocation.getToReference().toString(), method != null ? method.getName() : invocation.getMethodId(), durationMs);
            }
        }
    }

    protected void taskComplete(long startTimeNanos, Invocation invocation, @Nullable Method method)
    {
        if (performanceLoggingEnabled && logger.isWarnEnabled())
        {
            final long durationNanos = (System.nanoTime() - startTimeNanos);
            final double durationMs = durationNanos / 1_000_000.0;
            if (durationMs > slowTaskThresholdMs)
            {
                logger.warn("Slow chain: {}. {} in {} ms",
                        invocation.getToReference().toString(), method != null ? method.getName() : invocation.getMethodId(), durationMs);
            }
        }
    }

    public void setPerformanceLoggingEnabled(boolean performanceLoggingEnabled)
    {
        this.performanceLoggingEnabled = performanceLoggingEnabled;
    }

    public void setSlowInvokeThresholdMs(double slowInvokeThresholdMs)
    {
        this.slowInvokeThresholdMs = slowInvokeThresholdMs;
    }

    public void setSlowTaskThresholdMs(double slowTaskThresholdMs)
    {
        this.slowTaskThresholdMs = slowTaskThresholdMs;
    }
}
