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
import com.ea.orbit.actors.net.Pipeline;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Messaging;
import com.ea.orbit.actors.runtime.Peer;
import com.ea.orbit.actors.runtime.SerializationHandler;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;

public class ServerPeer extends Peer implements Startable
{
    private Stage stage;

    public ServerPeer()
    {

    }

    public Stage getStage()
    {
        return stage;
    }

    public void setStage(final Stage stage)
    {
        this.stage = stage;
    }

    public Task<?> start()
    {
        final Pipeline pipeline = getPipeline();
        pipeline.addLast(DefaultHandlers.EXECUTION, new ServerPeerExecutor(stage));
        pipeline.addLast(DefaultHandlers.MESSAGING, new Messaging());
        pipeline.addLast(DefaultHandlers.SERIALIZATION, new SerializationHandler(stage, getMessageSerializer()));
        pipeline.addLast(DefaultHandlers.NETWORK, getNetwork());
        return getPipeline().connect(null);
    }
}
