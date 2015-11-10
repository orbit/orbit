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

public class DefaultChannelHandlerContext implements ChannelHandlerContext
{
    ChannelHandler handler;
    DefaultChannelHandlerContext outbound;
    DefaultChannelHandlerContext inbound;

    private ChannelHandler handler()
    {
        return handler;
    }

    private DefaultChannelHandlerContext inbound()
    {
        return inbound;
    }

    private DefaultChannelHandlerContext outbound()
    {
        return outbound;
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(final Throwable cause)
    {
        final DefaultChannelHandlerContext inbound = inbound();
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
    public ChannelHandlerContext fireActive()
    {
        final DefaultChannelHandlerContext inbound = inbound();
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
    public ChannelHandlerContext fireInactive()
    {
        final DefaultChannelHandlerContext inbound = inbound();
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
    public ChannelHandlerContext fireEventTriggered(final Object event)
    {
        final DefaultChannelHandlerContext inbound = inbound();
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
    public ChannelHandlerContext fireRead(final Object msg)
    {
        final DefaultChannelHandlerContext inbound = inbound();
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
        final DefaultChannelHandlerContext outbound = outbound();
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
        final DefaultChannelHandlerContext outbound = outbound();
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
        final DefaultChannelHandlerContext outbound = outbound();
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
        final DefaultChannelHandlerContext outbound = outbound();
        try
        {
            return outbound.handler().write(outbound, msg);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    static final class TailContext extends DefaultChannelHandlerContext
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
            return Task.done();
        }
    }

    static final class HeadContext extends DefaultChannelHandlerContext
    {
        @Override
        public ChannelHandlerContext fireExceptionCaught(final Throwable cause)
        {
            return this;
        }

        @Override
        public ChannelHandlerContext fireActive()
        {
            return this;
        }

        @Override
        public ChannelHandlerContext fireInactive()
        {
            return this;
        }

        @Override
        public ChannelHandlerContext fireEventTriggered(final Object event)
        {
            return this;
        }

        @Override
        public ChannelHandlerContext fireRead(final Object msg)
        {
            return this;
        }
    }
}
