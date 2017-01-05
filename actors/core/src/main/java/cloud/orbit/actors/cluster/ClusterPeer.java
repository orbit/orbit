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

package cloud.orbit.actors.cluster;

import cloud.orbit.concurrent.Task;

import java.util.concurrent.ConcurrentMap;

/**
 * Represents a node connection to a cluster of orbit actor nodes.
 */
public interface ClusterPeer
{
    /**
     * Gets the cluster address of the local node represented by this peer.
     * Only available after joining.
     */
    NodeAddress localAddress();

    /**
     * Registers a callback that will be invoked every time the set of cluster nodes changed.
     * The first time it is called is after joining.
     *
     * @param viewListener the listener
     */
    void registerViewListener(ViewListener viewListener);

    /**
     * Registers a listener that will be called every time a cluster message is received.
     *
     * @param messageListener the listener
     */
    void registerMessageReceiver(MessageListener messageListener);

    /**
     * Sends a message to other cluster node.
     *
     * @param toAddress   the target node address
     * @param message the byte array representing the message
     */
    void sendMessage(NodeAddress toAddress, byte[] message);

    /**
     * Gets a reference to a distributed cache
     *
     * @param name cache name
     * @param <K>  the cache  key
     * @param <V>  the cache value
     * @return a cache
     */
    <K, V> ConcurrentMap<K, V> getCache(String name);

    /**
     * Joins a cluster
     *
     * @param clusterName the name/identifier of the cluster
     * @param nodeName the name of this node
     * @return future representing the completion of the joining process.
     */
    Task<?> join(String clusterName, String nodeName);

    /**
     * Pulses with the stage pulse
     *
     * @return future representing the completion of the pulse
     */
    default Task pulse() {return Task.done(); }

    /**
     * Leaves the cluster.
     */
    void leave();
}
