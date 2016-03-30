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

package cloud.orbit.actors.extensions;


import cloud.orbit.actors.net.Handler;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.concurrent.Task;

/**
 * Listener to extend the message pipeline.
 * Can intercept remote invocation, requests, responses, and binary messages
 * One of beforeHandlerName() or afterHandlerName() must be implemented.
 *
 * The default handler names are "head", "caching", "execution", "messaging", "serialization", "network", "tail"
 *
 * Handlers can only be inserted between "head" and "tail".
 *
 * @see cloud.orbit.actors.runtime.DefaultHandlers
 */
public interface PipelineExtension extends Handler, ActorExtension
{

    default String getBeforeHandlerName()
    {
        return null;
    }

    default String getAfterHandlerName()
    {
        return null;
    }

    default String getName()
    {
        return null;
    }

    @Override
    default void onExceptionCaught(final HandlerContext ctx, final Throwable cause) throws Exception
    {
        ctx.fireExceptionCaught(cause);
    }

    @Override
    default void onActive(final HandlerContext ctx) throws Exception
    {
        ctx.fireActive();
    }

    @Override
    default void onInactive(final HandlerContext ctx) throws Exception
    {
        ctx.fireInactive();
    }

    @Override
    default void onEventTriggered(final HandlerContext ctx, final Object evt) throws Exception
    {
        ctx.fireEventTriggered(evt);
    }


    @Override
    default void onRead(final HandlerContext ctx, final Object msg) throws Exception
    {
        ctx.fireRead(msg);
    }

    @Override
    default void onRegistered(HandlerContext ctx) throws Exception
    {

    }

    @Override
    default Task connect(final HandlerContext ctx, final Object param) throws Exception
    {
        return ctx.connect(param);
    }

    @Override
    default Task disconnect(final HandlerContext ctx) throws Exception
    {
        return ctx.disconnect();
    }

    @Override
    default Task close(final HandlerContext ctx) throws Exception
    {
        return ctx.close();
    }

    @Override
    default Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        return ctx.write(msg);
    }
}
