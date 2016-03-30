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

package cloud.orbit.actors.net;

import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;

/**
 * Protocol chain for orbit messages.
 * <br>
 * Inspired in netty.io and apache mina.
 */
public class DefaultPipeline implements Pipeline
{
    private final DefaultHandlerContext.HeadContext head;
    private final DefaultHandlerContext tail;
    private BasicRuntime runtime;

    public DefaultPipeline()
    {
        head = new DefaultHandlerContext.HeadContext(this);
        tail = new DefaultHandlerContext.TailContext(this);

        head.handler = new HandlerAdapter();
        head.name = DefaultHandlers.HEAD;
        head.outbound = tail;

        tail.handler = new HandlerAdapter();
        tail.name = DefaultHandlers.TAIL;
        tail.inbound = head;
    }

    public DefaultPipeline(final BasicRuntime runtime)
    {
        this();
        this.runtime = runtime;
    }

    public BasicRuntime getRuntime()
    {
        return runtime;
    }

    void addContextBefore(DefaultHandlerContext base, DefaultHandlerContext newContext)
    {
        if (base == head)
        {
            base = head.outbound;
        }
        newContext.inbound = base.inbound;
        newContext.outbound = base;
        base.inbound.outbound = newContext;
        base.inbound = newContext;
        try
        {
            newContext.handler.onRegistered(newContext);
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    @Override
    public boolean isActive()
    {
        return head.isActive();
    }

    @Override
    public void addFirst(final String name, final Handler handler)
    {
        final DefaultHandlerContext ctx = new DefaultHandlerContext(this);
        ctx.handler = handler;
        ctx.name = name;
        addContextBefore(head.outbound, ctx);
    }

    @Override
    public void addLast(final String name, final Handler handler)
    {
        final DefaultHandlerContext ctx = new DefaultHandlerContext(this);
        ctx.handler = handler;
        ctx.name = name;
        addContextBefore(tail, ctx);
    }

    DefaultHandlerContext findFirstHandlerContext(String name)
    {
        for (DefaultHandlerContext ctx = head; ctx != null; ctx = ctx.outbound)
        {
            if (ctx.name != null && ctx.name.equals(name))
            {
                return ctx;
            }
        }
        return null;
    }

    DefaultHandlerContext findLastHandlerContext(String name)
    {
        for (DefaultHandlerContext ctx = tail; ctx != null; ctx = ctx.inbound)
        {
            if (ctx.name != null && ctx.name.equals(name))
            {
                return ctx;
            }
        }
        return null;
    }


    @Override
    public void addHandlerBefore(final String base, final String name, final Handler handler)
    {
        final DefaultHandlerContext ctx = new DefaultHandlerContext(this);
        ctx.handler = handler;
        ctx.name = name;
        addContextBefore(findFirstHandlerContext(base), ctx);
    }

    @Override
    public void addHandlerAfter(final String base, final String name, final Handler handler)
    {
        final DefaultHandlerContext ctx = new DefaultHandlerContext(this);
        ctx.handler = handler;
        ctx.name = name;
        addContextBefore(findLastHandlerContext(base).outbound, ctx);
    }

    @Override
    public Task<Void> write(Object message)
    {
        return head.write(message);
    }

    @Override
    public Task<Void> connect(Object param)
    {
        return head.connect(param);
    }

    @Override
    public Task<Void> disconnect()
    {
        return head.disconnect();
    }

    @Override
    public Task<Void> close()
    {
        return head.close();
    }
}
