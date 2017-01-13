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
import cloud.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHandlerContext implements HandlerContext
{
    private static Logger logger = LoggerFactory.getLogger(DefaultHandlerContext.class);
    Handler handler;
    DefaultHandlerContext outbound;
    DefaultHandlerContext inbound;
    String name;
    DefaultPipeline pipeline;

    public DefaultHandlerContext(final DefaultPipeline pipeline)
    {
        this.pipeline = pipeline;
    }

    @Override
    public BasicRuntime getRuntime()
    {
        return pipeline.getRuntime();
    }

    @Override
    public HandlerContext fireExceptionCaught(final Throwable cause)
    {
        try
        {
            inbound.handler.onExceptionCaught(inbound, cause);
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
        try
        {
            inbound.handler.onActive(inbound);
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
        try
        {
            inbound.handler.onInactive(inbound);
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
        try
        {
            inbound.handler.onEventTriggered(inbound, event);
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
        try
        {
            inbound.handler.onRead(inbound, msg);
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
        logger.debug("Connecting handler {}...", name);
        try
        {
            return outbound.handler.connect(outbound, param);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    @Override
    public Task disconnect()
    {
        try
        {
            return outbound.handler.disconnect(outbound);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    @Override
    public Task close()
    {
        try
        {
            return outbound.handler.close(outbound);
        }
        catch (Exception e)
        {
            return Task.fromException(e);
        }
    }

    @Override
    public Task write(final Object msg)
    {
        try
        {
            return outbound.handler.write(outbound, msg);
        }
        catch (Exception e)
        {
            if (logger.isErrorEnabled())
            {
                logger.error("Error writing: ", e);
            }
            return Task.fromException(e);
        }
    }

    static final class TailContext extends DefaultHandlerContext
    {
        public TailContext(final DefaultPipeline pipeline)
        {
            super(pipeline);
        }

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
            if (logger.isWarnEnabled())
            {
                logger.warn("Unprocessed write " + msg);
            }
            return Task.done();
        }
    }

    static class HeadContext extends DefaultHandlerContext
    {
        static Logger logger = org.slf4j.LoggerFactory.getLogger(HeadContext.class);
        private boolean active;

        public HeadContext(final DefaultPipeline pipeline)
        {
            super(pipeline);
        }

        @Override
        public HandlerContext fireExceptionCaught(final Throwable cause)
        {
            logger.error("Uncaught exception", cause);
            return this;
        }

        @Override
        public HandlerContext fireActive()
        {
            active = true;
            return this;
        }

        @Override
        public HandlerContext fireInactive()
        {
            active = false;
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

        public boolean isActive()
        {
            return active;
        }
    }
}
