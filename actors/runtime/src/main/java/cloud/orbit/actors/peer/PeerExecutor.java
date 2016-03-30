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

package cloud.orbit.actors.peer;

import cloud.orbit.actors.net.HandlerAdapter;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.InternalUtils;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.LocalObjects;
import cloud.orbit.actors.runtime.ObjectInvoker;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ea.async.Async.await;

public class PeerExecutor extends HandlerAdapter
{
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected LocalObjects objects;
    protected BasicRuntime runtime;

    public PeerExecutor()
    {

    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            return writeInvocation(ctx, (Invocation) msg);
        }
        return ctx.write(msg);
    }

    public Task writeInvocation(final HandlerContext ctx, final Invocation invocation) throws Exception
    {
        LocalObjects.LocalObjectEntry localObjectEntry = objects.findLocalObjectByReference(invocation.getToReference());
        if (localObjectEntry != null)
        {
            return scheduleLocalInvocation(localObjectEntry, invocation);
        }
        return ctx.write(invocation);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRead(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            onInvoke(ctx, (Invocation) msg);
        }
        else
        {
            ctx.fireRead(msg);
        }
    }

    @SuppressWarnings("unchecked")
    protected void onInvoke(final HandlerContext ctx, final Invocation invocation)
    {
        LocalObjects.LocalObjectEntry localObjectEntry = objects.findLocalObjectByReference(invocation.getToReference());
        if (localObjectEntry != null)
        {
            scheduleLocalInvocation(localObjectEntry, invocation);
            return;
        }
        ctx.fireRead(invocation);
    }

    /**
     * @return the result of the method called
     */
    @SuppressWarnings("unchecked")
    public Task scheduleLocalInvocation(final LocalObjects.LocalObjectEntry<Object> localObjectEntry, final Invocation invocation)
    {
        ObjectInvoker invoker = runtime.getInvoker(RemoteReference.getInterfaceId(invocation.getToReference()));
        return localObjectEntry.run(target ->
            performLocalInvocation(invocation, invoker, target)
        );

    }

    protected Task<Object> performLocalInvocation(final Invocation invocation, final ObjectInvoker invoker, final LocalObjects.LocalObjectEntry target)
    {
        Task result = invoker.safeInvoke(target.getObject(), invocation.getMethodId(), invocation.getParams());
        try
        {
            // must await to hold the execution serializer
            await(result);
        }
        catch (Throwable ex)
        {
            // handled bellow;
        }
        if (invocation.getCompletion() != null)
        {
            InternalUtils.linkFutures(result, invocation.getCompletion());
        }
        return result;
    }


    public void setObjects(final LocalObjects objects)
    {
        this.objects = objects;
    }

    public void setRuntime(final BasicRuntime runtime)
    {
        this.runtime = runtime;
        logger = runtime.getLogger(this);
    }
}
