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

import cloud.orbit.actors.cluster.ClusterPeer;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.net.HandlerAdapter;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.concurrent.Task;
import cloud.orbit.tuples.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterHandler extends HandlerAdapter
{
    private static Logger logger = LoggerFactory.getLogger(ClusterHandler.class);
    private ClusterPeer clusterPeer;
    private String clusterName;
    private String nodeName;

    public ClusterHandler(final ClusterPeer clusterPeer, final String clusterName, final String nodeName)
    {
        this.clusterPeer = clusterPeer;
        this.clusterName = clusterName;
        this.nodeName = nodeName;
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (!(msg instanceof Pair))
        {
            return ctx.write(msg);
        }
        Pair<NodeAddress, byte[]> message = (Pair<NodeAddress, byte[]>) msg;
        clusterPeer.sendMessage(message.getLeft(), message.getRight());
        return Task.done();
    }

    @Override
    public Task connect(final HandlerContext ctx, final Object param) throws Exception
    {
        logger.info("Connecting handler ClusterHandler...");
        clusterPeer.registerMessageReceiver((n, m) -> ctx.fireRead(Pair.of(n, m)));
        return clusterPeer.join(clusterName, nodeName).thenRun(() ->
                {
                    try
                    {
                        ctx.fireActive();
                    }
                    catch (Throwable ex)
                    {
                        logger.error("Error handling message", ex);
                    }
                }
        );
    }

    @Override
    public Task close(final HandlerContext ctx) throws Exception
    {
        logger.info("Closing ClusterHandler... ");
        clusterPeer.leave();
        return super.close(ctx);
    }
}
