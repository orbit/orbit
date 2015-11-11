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

package com.ea.orbit.actors.net;

import com.ea.orbit.concurrent.Task;

import org.slf4j.Logger;

public class DefaultHandlerContext implements HandlerContext
{
    Handler handler;
    DefaultHandlerContext outbound;
    DefaultHandlerContext inbound;
    String name;

    private Handler handler()
    {
        return handler;
    }

    private DefaultHandlerContext inbound()
    {
        return inbound;
    }

    private DefaultHandlerContext outbound()
    {
        return outbound;
    }

    @Override
    public HandlerContext fireExceptionCaught(final Throwable cause)
    {
        final DefaultHandlerContext inbound = inbound();
        try
        {
            inbound.handler().onExceptionCaught(inbound, cause);
        }
        catch (Exception e)
        {
            inbound.fireExceptionCaught(e);
        }
        return this;
    }

    @Override
    public HandlerContext fireActive()
    {
        final DefaultHandlerContext inbound = inbound();
        try
        {
            inbound.handler().onActive(inbound);
        }
        catch (Exception e)
        {
            inbound.fireExceptionCaught(e);
        }
        return this;
    }

    @Override
    public HandlerContext fireInactive()
    {
        final DefaultHandlerContext inbound = inbound();
        try
        {
            inbound.handler().onInactive(inbound);
        }
        catch (Exception e)
        {
            inbound.fireExceptionCaught(e);
        }
        return this;
    }

    @Override
    public HandlerContext fireEventTriggered(final Object event)
    {
        final DefaultHandlerContext inbound = inbound();
        try
        {
            inbound.handler().onEventTriggered(inbound, event);
        }
        catch (Exception e)
        {
            inbound.fireExceptionCaught(e);
        }
        return this;
    }

    @Override
    public HandlerContext fireRead(final Object msg)
    {
        final DefaultHandlerContext inbound = inbound();
        try
        {
            inbound.handler().onRead(inbound, msg);
        }
        catch (Exception e)
        {
            inbound.fireExceptionCaught(e);
        }
        return this;
    }

    @Override
    public Task connect(final Object param)
    {
        final DefaultHandlerContext outbound = outbound();
        try
        {
            return outbound.handler().connect(outbound, param);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    @Override
    public Task disconnect()
    {
        final DefaultHandlerContext outbound = outbound();
        try
        {
            return outbound.handler().disconnect(outbound);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    @Override
    public Task close()
    {
        final DefaultHandlerContext outbound = outbound();
        try
        {
            return outbound.handler().close(outbound);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    @Override
    public Task write(final Object msg)
    {
        final DefaultHandlerContext outbound = outbound();
        try
        {
            final Handler handler = outbound.handler();
            return handler.write(outbound, msg);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    static final class TailContext extends DefaultHandlerContext
    {
        @Override
        public Task connect(final Object param)
        {
            return Task.done();
        }

        @Override
        public Task disconnect()
        {
            return Task.done();
        }

        @Override
        public Task close()
        {
            return Task.done();
        }

        @Override
        public Task write(final Object msg)
        {
            // TODO: logger.warn("Unprocessed write " + msg);
            return Task.done();
        }
    }

    static final class HeadContext extends DefaultHandlerContext
    {
        static Logger logger = org.slf4j.LoggerFactory.getLogger(HeadContext.class);

        @Override
        public HandlerContext fireExceptionCaught(final Throwable cause)
        {
            logger.error("Uncaught exception", cause);
            return this;
        }

        @Override
        public HandlerContext fireActive()
        {
            return this;
        }

        @Override
        public HandlerContext fireInactive()
        {
            return this;
        }

        @Override
        public HandlerContext fireEventTriggered(final Object event)
        {
            return this;
        }

        @Override
        public HandlerContext fireRead(final Object msg)
        {
            return this;
        }
    }
}
