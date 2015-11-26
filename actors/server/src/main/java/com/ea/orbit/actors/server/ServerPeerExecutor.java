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

package com.ea.orbit.actors.server;

import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.peer.PeerExecutor;
import com.ea.orbit.actors.runtime.Invocation;
import com.ea.orbit.actors.runtime.LocalObjects;
import com.ea.orbit.actors.runtime.ObjectInvoker;
import com.ea.orbit.actors.runtime.Utils;
import com.ea.orbit.concurrent.Task;

class ServerPeerExecutor extends PeerExecutor
{
    private final Stage stage;

    public ServerPeerExecutor(final Stage stage)
    {
        this.stage = stage;
        setRuntime(stage);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onInvoke(final HandlerContext ctx, final Invocation invocation)
    {
        LocalObjects.LocalObjectEntry localObjectEntry = objects.findLocalObjectByReference(invocation.getToReference());
        if (localObjectEntry != null)
        {
            // local to the connection
            scheduleLocalInvocation(localObjectEntry, invocation);
            return;
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("Forwarding to the server: {}", invocation);
        }
        // forward to the server
        final Task<Void> write = stage.getPipeline().write(invocation);
        if (invocation.getCompletion() != null)
        {
            Utils.linkFutures(write, invocation.getCompletion());
        }
    }

    @Override
    protected Task<Object> performLocalInvocation(final Invocation invocation, final ObjectInvoker invoker, final Object target)
    {
        stage.bind();
        return super.performLocalInvocation(invocation, invoker, target);
    }
}
