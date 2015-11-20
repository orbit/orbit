/*
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

package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.annotation.PreferLocalPlacement;
import com.ea.orbit.actors.annotation.StatelessWorker;
import com.ea.orbit.actors.cluster.ClusterPeer;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Hosting implements NodeCapabilities, Startable
{
    private static final Logger logger = LoggerFactory.getLogger(Hosting.class);
    private NodeTypeEnum nodeType;
    private ClusterPeer clusterPeer;

    private volatile Map<NodeAddress, NodeInfo> activeNodes = new HashMap<>(0);
    private volatile List<NodeInfo> serverNodes = new ArrayList<>(0);
    private final Object serverNodesUpdateMutex = new Object();
    private Execution execution;
    private Cache<ActorKey, NodeAddress> localAddressCache = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private volatile ConcurrentMap<ActorKey, NodeAddress> distributedDirectory;
    @Config("orbit.actors.timeToWaitForServersMillis")
    private long timeToWaitForServersMillis = 30000;
    private Random random = new Random();

    private TreeMap<String, NodeInfo> consistentHashNodeTree = new TreeMap<>();

    public Hosting()
    {
        //
    }

    public long getTimeToWaitForServersMillis()
    {
        return timeToWaitForServersMillis;
    }

    public void setTimeToWaitForServersMillis(final long timeToWaitForServersMillis)
    {
        this.timeToWaitForServersMillis = timeToWaitForServersMillis;
    }

    public void setExecution(final Execution execution)
    {
        this.execution = execution;
    }

    public void setNodeType(final NodeTypeEnum nodeType)
    {
        this.nodeType = nodeType;
    }

    public List<NodeAddress> getAllNodes()
    {
        return Collections.unmodifiableList(new ArrayList<>(activeNodes.keySet()));
    }

    public List<NodeAddress> getServerNodes()
    {
        final ArrayList<NodeAddress> set = new ArrayList<>(serverNodes.size());
        for (NodeInfo s : serverNodes)
        {
            set.add(s.address);
        }
        return Collections.unmodifiableList(set);
    }

    public Task<?> notifyStateChange()
    {
        return Task.allOf(activeNodes.values().stream().map(info ->
                info.nodeCapabilities.nodeModeChanged(clusterPeer.localAddress(), execution.getState())));
    }

    public NodeAddress getNodeAddress()
    {
        return clusterPeer.localAddress();
    }

    private static class NodeInfo
    {
        boolean active;
        NodeAddress address;
        NodeState state = NodeState.RUNNING;
        NodeCapabilities nodeCapabilities;
        boolean cannotHostActors;
        final ConcurrentHashMap<String, Integer> canActivate = new ConcurrentHashMap<>();

        public NodeInfo(final NodeAddress address)
        {
            this.address = address;
        }
    }

    @Override
    public Task<Integer> canActivate(String interfaceName)
    {
        if (nodeType == NodeTypeEnum.CLIENT || execution.getState() != NodeState.RUNNING)
        {
            return Task.fromValue(actorSupported_noneSupported);
        }
        return Task.fromValue(execution.canActivateActor(interfaceName) ? actorSupported_yes
                : actorSupported_no);
    }

    @Override
    public Task<Void> nodeModeChanged(final NodeAddress nodeAddress, final NodeState newState)
    {
        final NodeInfo node = activeNodes.get(nodeAddress);
        if (node != null)
        {
            node.state = newState;
            if (node.state != NodeState.RUNNING)
            {
                // clear list of actors this node can activate
                node.canActivate.clear();
            }
        }
        return Task.done();
    }

    public Task<Void> moved(ActorKey actorKey, NodeAddress oldAddress, NodeAddress newAddress)
    {
        localAddressCache.put(actorKey, newAddress);
        return Task.done();
    }

    public void setClusterPeer(final ClusterPeer clusterPeer)
    {
        this.clusterPeer = clusterPeer;
    }

    public Task<Void> start()
    {
        clusterPeer.registerViewListener(v -> onClusterViewChanged(v));
        return Task.done();
    }

    private void onClusterViewChanged(final Collection<NodeAddress> nodes)
    {
        HashMap<NodeAddress, NodeInfo> oldNodes = new HashMap<>(activeNodes);
        HashMap<NodeAddress, NodeInfo> newNodes = new HashMap<>(nodes.size());
        List<NodeInfo> justAddedNodes = new ArrayList<>(Math.max(1, nodes.size() - oldNodes.size()));

        final TreeMap<String, NodeInfo> newHashes = new TreeMap<>();

        for (final NodeAddress a : nodes)
        {
            NodeInfo nodeInfo = oldNodes.remove(a);
            if (nodeInfo == null)
            {
                nodeInfo = new NodeInfo(a);
                nodeInfo.nodeCapabilities = execution.createReference(a, NodeCapabilities.class, "");
                nodeInfo.active = true;
                activeNodes.put(a, nodeInfo);
                justAddedNodes.add(nodeInfo);
            }
            newNodes.put(a, nodeInfo);

            final String addrHexStr = a.asUUID().toString();
            for (int i = 0; i < 10; i++)
            {
                final String hash = getHash(addrHexStr + ":" + i);
                newHashes.put(hash, nodeInfo);
            }
        }
        // nodes that were removed
        for (NodeInfo oldNodeInfo : oldNodes.values())
        {
            oldNodeInfo.active = false;
        }
        activeNodes = newNodes;
        consistentHashNodeTree = newHashes;
        updateServerNodes();
        // TODO notify someone? (NodeInfo oldNodeInfo : oldNodes.values()) { ... }
    }

    private void updateServerNodes()
    {
        synchronized (serverNodesUpdateMutex)
        {
            this.serverNodes = activeNodes.values().stream().filter(
                    nodeInfo -> nodeInfo.active && !nodeInfo.cannotHostActors).collect(Collectors.toList());
            if (serverNodes.size() > 0)
            {
                serverNodesUpdateMutex.notifyAll();
            }
        }
    }

    public Task<NodeAddress> locateActor(final Addressable reference, final boolean forceActivation)
    {
        return (forceActivation) ? locateAndActivateActor(reference) : locateActiveActor(reference);
    }


    private Task<NodeAddress> locateActiveActor(final Addressable actorReference)
    {
        ActorKey addressable = new ActorKey(((RemoteReference) actorReference)._interfaceClass().getName(),
                String.valueOf(((RemoteReference) actorReference).id));
        NodeAddress address = localAddressCache.getIfPresent(addressable);
        if (address != null && activeNodes.containsKey(address))
        {
            return Task.fromValue(address);
        }
        return Task.fromValue(null);
    }

    private Task<NodeAddress> locateAndActivateActor(final Addressable actorReference)
    {
        final ActorKey addressable = new ActorKey(((RemoteReference) actorReference)._interfaceClass().getName(),
                String.valueOf(((RemoteReference) actorReference).id));

        final Class<?> interfaceClass = ((RemoteReference<?>) actorReference)._interfaceClass();
        final String interfaceClassName = interfaceClass.getName();

        // First we handle Stateless Worker as it's a special case
        if (interfaceClass.isAnnotationPresent(StatelessWorker.class))
        {
            // Do we want to place locally?
            if (shouldPlaceLocally(interfaceClass))
            {
                return Task.fromValue(clusterPeer.localAddress());
            }
            else
            {
                return Task.fromValue(selectNode(interfaceClassName, true));
            }
        }

        // Get the existing activation from the local cache (if any)
        NodeAddress address = localAddressCache.getIfPresent(addressable);

        // Is this actor already activated and in the local cache? If so, we're done
        if (address != null && activeNodes.containsKey(address))
        {
            return Task.fromValue(address);
        }

        // There is no existing activation at this time or it's not in the local cache
        final CompletableFuture<NodeAddress> async = CompletableFuture.supplyAsync(() ->
        {
            NodeAddress nodeAddress = null;

            // Get the distributed cache if needed
            if (distributedDirectory == null)
            {
                synchronized (this)
                {
                    if (distributedDirectory == null)
                    {
                        distributedDirectory = clusterPeer.getCache("distributedDirectory");
                    }
                }
            }

            // Get the existing activation from the distributed cache (if any)
            nodeAddress = distributedDirectory.get(addressable);
            if (nodeAddress != null)
            {
                // Target node still valid?
                if (activeNodes.containsKey(nodeAddress))
                {
                    // Cache locally
                    localAddressCache.put(addressable, nodeAddress);
                    return nodeAddress;
                }
                else
                {
                    // Target node is now dead, remove this activation from distributed cache
                    distributedDirectory.remove(addressable, nodeAddress);
                }
            }
            nodeAddress = null;

            // Should place locally?
            if (shouldPlaceLocally(interfaceClass))
            {
                nodeAddress = clusterPeer.localAddress();
            }

            // Do we have a target node yet?
            if (nodeAddress == null)
            {
                // If not, select randomly
                nodeAddress = selectNode(interfaceClassName, true);
            }

            // Push our selection to the distributed cache (if possible)
            NodeAddress otherNodeAddress = distributedDirectory.putIfAbsent(addressable, nodeAddress);

            // Someone else beat us to placement, use that node
            if (otherNodeAddress != null)
            {
                nodeAddress = otherNodeAddress;
            }

            // Add to local cache
            localAddressCache.put(addressable, nodeAddress);

            return nodeAddress;

        }, execution.getExecutor());

        return Task.from(async);

    }

    private NodeAddress selectNode(final String interfaceClassName, boolean allowToBlock)
    {
        List<NodeInfo> potentialNodes;
        long start = System.currentTimeMillis();

        while (true)
        {
            if (System.currentTimeMillis() - start > timeToWaitForServersMillis)
            {
                String err = "Timeout waiting for a server capable of handling: " + interfaceClassName;
                logger.error(err);
                throw new UncheckedException(err);
            }

            List<NodeInfo> currentServerNodes = serverNodes;

            potentialNodes = currentServerNodes.stream()
                    .filter(n -> (!n.cannotHostActors && n.state == NodeState.RUNNING)
                            && NodeCapabilities.actorSupported_no != n.canActivate.getOrDefault(interfaceClassName, NodeCapabilities.actorSupported_yes))
                    .collect(Collectors.toList());

            if (potentialNodes.size() == 0)
            {
                if (!allowToBlock)
                {
                    return null;
                }
                // waits for servers
                synchronized (serverNodesUpdateMutex)
                {
                    if (serverNodes.size() == 0)
                    {
                        try
                        {
                            serverNodesUpdateMutex.wait(5);
                        }
                        catch (InterruptedException e)
                        {
                            throw new UncheckedException(e);
                        }
                    }
                }
            }
            else
            {
                NodeInfo nodeInfo = potentialNodes.get(random.nextInt(potentialNodes.size()));

                Integer canActivate = nodeInfo.canActivate.get(interfaceClassName);
                if (canActivate == null)
                {
                    // ask if the node can activate this type of actor.
                    try
                    {
                        canActivate = nodeInfo.nodeCapabilities.canActivate(interfaceClassName).join();
                        if (canActivate == actorSupported_noneSupported)
                        {
                            nodeInfo.cannotHostActors = true;
                            // jic
                            nodeInfo.canActivate.put(interfaceClassName, actorSupported_no);
                        }
                        else
                        {
                            nodeInfo.canActivate.put(interfaceClassName, canActivate);
                        }
                    }
                    catch (Exception ex)
                    {
                        logger.error("Error locating server for " + interfaceClassName, ex);
                        continue;
                    }
                }
                if (canActivate == actorSupported_yes)
                {
                    return nodeInfo.address;
                }
            }
        }
    }

    private boolean shouldPlaceLocally(final Class<?> interfaceClass)
    {
        final String interfaceClassName = interfaceClass.getName();

        if (interfaceClass.isAnnotationPresent(PreferLocalPlacement.class) &&
                nodeType == NodeTypeEnum.SERVER && execution.canActivateActor(interfaceClassName))
        {
            final int percentile = interfaceClass.getAnnotation(PreferLocalPlacement.class).percentile();
            if (random.nextInt(100) < percentile)
            {
                return true;
            }
        }

        return false;
    }

    private String getHash(final String key)
    {
        try
        {
            MessageDigest md = null;
            md = MessageDigest.getInstance("SHA-256");
            md.update(key.getBytes("UTF-8"));
            byte[] digest = md.digest();
            return String.format("%064x", new java.math.BigInteger(1, digest));
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    /**
     * Uses consistent hashing to determine the "owner" of a certain key.
     *
     * @param key
     * @return the NodeAddress of the node that's supposed to own the key.
     */
    public NodeAddress getConsistentHashOwner(final String key)
    {
        final String keyHash = getHash(key);
        final TreeMap<String, NodeInfo> currentHashes = consistentHashNodeTree;
        Map.Entry<String, NodeInfo> info = currentHashes.ceilingEntry(keyHash);
        if (info == null)
        {
            info = currentHashes.firstEntry();
        }
        return info.getValue().address;
    }

    /**
     * Uses consistent hashing to determine this node is the "owner" of a certain key.
     *
     * @param key
     * @return true if this node is assigned to "own" the key.
     */
    public boolean isConsistentHashOwner(final String key)
    {
        final NodeAddress owner = getConsistentHashOwner(key);
        return clusterPeer.localAddress().equals(owner);
    }

}
