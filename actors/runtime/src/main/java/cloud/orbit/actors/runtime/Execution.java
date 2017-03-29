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

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.exceptions.ObserverNotFound;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.concurrent.Task;
import cloud.orbit.lifecycle.Startable;

import java.util.Objects;

public class Execution extends AbstractExecution implements Startable
{
    private Stage runtime;
    private LocalObjects objects;

    private InvocationHandler invocationHandler;

    @Override
    public Task<Void> cleanup()
    {
        return Task.done();
    }

    public void setRuntime(final Stage runtime)
    {
        this.runtime = runtime;
        this.logger = runtime.getLogger(this);
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        return ctx.write(msg);
    }

    @Override
    public void onRead(HandlerContext ctx, Object msg)
    {
        if (msg instanceof Invocation)
        {
            onInvocation(ctx, (Invocation) msg);
        }
        else
        {
            ctx.fireRead(msg);
        }
    }

    @SuppressWarnings("unchecked")
    protected void onInvocation(final HandlerContext ctx, final Invocation invocation)
    {
        final RemoteReference toReference = invocation.getToReference();
        final LocalObjects.LocalObjectEntry<Object> entry = objects.findLocalObjectByReference(toReference);
        if (entry != null)
        {
            final Task<Object> result = InternalUtils.safeInvoke(() -> entry.run(
                    target -> performInvocation(ctx, invocation, entry, target)));
            // this has to be done here because of exceptions that can occur before performInvocation is even called.
            if (invocation.getCompletion() != null)
            {
                InternalUtils.linkFuturesOnError(result, invocation.getCompletion());
            }
        }
        else
        {
            if (toReference instanceof Actor)
            {
                // on activate will handle the completion;
                InternalUtils.safeInvoke(() -> executionSerializer.offerJob(toReference,
                        () -> onActivate(ctx, invocation), maxQueueSize));
            }
            else
            {
                // missing actor observer
                invocation.setHops(invocation.getHops() + 1);
                if (invocation.getCompletion() != null)
                {
                    invocation.getCompletion().completeExceptionally(new ObserverNotFound());
                }
            }
        }
    }

    private Task<Void> onActivate(HandlerContext ctx, final Invocation invocation)
    {
        // this must run serialized by the remote reference key.
        LocalObjects.LocalObjectEntry<Object> entry = objects.findLocalObjectByReference(invocation.getToReference());
        if (entry == null)
        {
            objects.registerLocalObject(invocation.getToReference());
            entry = objects.findLocalObjectByReference(invocation.getToReference());
        }
        // queues the invocation
        final LocalObjects.LocalObjectEntry<Object> theEntry = entry;
        final Task result = entry.run(target -> performInvocation(ctx, invocation, theEntry, target));
        if (invocation.getCompletion() != null)
        {
            InternalUtils.linkFuturesOnError(result, invocation.getCompletion());
        }
        // yielding since we blocked the entry before running on activate (serialized execution)
        return Task.done();
    }

    @SuppressWarnings("unchecked")
    protected Task<Object> performInvocation(
            HandlerContext ctx,
            final Invocation invocation,
            final LocalObjects.LocalObjectEntry entry,
            final LocalObjects.LocalObjectEntry target)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Invoking {} ", invocation);
        }
        try
        {
            if (target == null || target.isDeactivated())
            {
                // if the entry is deactivated, forward the message back to the net.
                ctx.write(invocation);
                return Task.fromValue(null);
            }
            if (target.getObject() == null)
            {
                if (target instanceof ObserverEntry)
                {
                    return Task.fromException(new ObserverNotFound());
                }
                ctx.write(invocation);
                return Task.fromValue(null);
            }

            final ObjectInvoker invoker = DefaultDescriptorFactory.get().getInvoker(target.getObject().getClass());
            return invocationHandler.invoke(runtime, invocation, entry, target, invoker);
        }
        catch (Throwable exception)
        {
            if (logger.isErrorEnabled())
            {
                logger.error("Unknown application error. ", exception);
            }
            if (invocation.getCompletion() != null)
            {
                invocation.getCompletion().completeExceptionally(exception);
            }
            return Task.fromException(exception);
        }
    }

    public void setInvocationHandler(final InvocationHandler invocationHandler)
    {
        this.invocationHandler = Objects.requireNonNull(invocationHandler);
    }

    public void setObjects(final LocalObjects objects)
    {
        this.objects = Objects.requireNonNull(objects);
    }

}