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
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;

import java.util.Map;

public class Execution extends AbstractExecution implements Startable
{
    private Stage runtime;
    private LocalObjects objects;

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
        final LocalObjects.LocalObjectEntry entry = objects.findLocalObjectByReference(toReference);
        if (entry != null)
        {
            final Task<Object> result = InternalUtils.safeInvoke(() -> entry.run(target -> performInvocation(ctx, invocation, entry, target)));
            // this has to be done here because of exceptions that can occur before performInvocation is even called.
            if (invocation.getCompletion() != null)
            {
                InternalUtils.linkFutures(result, invocation.getCompletion());
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
                invocation.setHops(invocation.getHops() + 1);
                // return the message to hosting, hosting is responsible for activating the actor locally.
                ctx.write(invocation);
            }
        }
    }

    private Task<Void> onActivate(HandlerContext ctx, final Invocation invocation)
    {
        // this must run serialized by the remote reference key.
        LocalObjects.LocalObjectEntry entry = objects.findLocalObjectByReference(invocation.getToReference());
        if (entry == null)
        {
            objects.registerLocalObject(invocation.getToReference(), null);
            entry = objects.findLocalObjectByReference(invocation.getToReference());
        }
        // queues the invocation
        final LocalObjects.LocalObjectEntry theEntry = entry;
        final Task result = entry.run(target -> performInvocation(ctx, invocation, theEntry, target));
        if (invocation.getCompletion() != null)
        {
            InternalUtils.linkFutures(result, invocation.getCompletion());
        }
        // yielding since we blocked the entry before running on activate (serialized execution)
        return Task.done();
    }

    @SuppressWarnings("unchecked")
    protected <T> Task<Object> performInvocation(HandlerContext ctx, final Invocation invocation, final LocalObjects.LocalObjectEntry entry, final T target)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Invoking {} ", invocation);
        }
        try
        {
            runtime.bind();
            // TODO: if entry is deactivated, forward the message back to the net.
            final ObjectInvoker invoker = DefaultDescriptorFactory.get().getInvoker(target.getClass());

            final ActorTaskContext context = ActorTaskContext.current();
            if (invocation.getHeaders() != null && invocation.getHeaders().size() > 0 && runtime.getStickyHeaders() != null)
            {
                for (Map.Entry e : invocation.getHeaders().entrySet())
                {
                    if (runtime.getStickyHeaders().contains(e.getKey()))
                    {
                        context.setProperty(String.valueOf(e.getKey()), e.getValue());
                    }
                }
            }
            return invoker.safeInvoke(target, invocation.getMethodId(), invocation.getParams());
        }
        catch (Throwable exception)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Unknown application error. ", exception);
            }
            if (!invocation.isOneWay() && invocation.getCompletion() != null)
            {
                invocation.getCompletion().completeExceptionally(exception);
            }
            return Task.fromException(exception);
        }
    }

    public void setObjects(final LocalObjects objects)
    {
        this.objects = objects;
    }

}
