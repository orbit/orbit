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

import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.annotation.PreferLocalPlacement;
import com.ea.orbit.actors.annotation.StatelessWorker;
import com.ea.orbit.actors.cluster.ClusterPeer;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.extensions.PipelineExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.annotation.OnlyIfActivated;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ea.orbit.async.Await.await;

public class Hosting implements NodeCapabilities, Startable, PipelineExtension
{
    private Logger logger = LoggerFactory.getLogger(Hosting.class);
    private NodeTypeEnum nodeType;
    private ClusterPeer clusterPeer;

    private volatile Map<NodeAddress, NodeInfo> activeNodes = new HashMap<>(0);
    private volatile List<NodeInfo> serverNodes = new ArrayList<>(0);
    private final Object serverNodesUpdateMutex = new Object();
    private Stage stage;
    private Cache<RemoteReference<?>, NodeAddress> localAddressCache = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    // don't use RemoteReferences, better to restrict keys to a small set of classes.
    private volatile ConcurrentMap<RemoteKey, NodeAddress> distributedDirectory;
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

    public void setStage(final Stage stage)
    {
        this.stage = stage;
        logger = stage.getLogger(this);
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
                info.nodeCapabilities.nodeModeChanged(clusterPeer.localAddress(), stage.getState())));
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
        if (nodeType == NodeTypeEnum.CLIENT || stage.getState() == NodeState.STOPPED)
        {
            return Task.fromValue(actorSupported_noneSupported);
        }
        return Task.fromValue(stage.canActivateActor(interfaceName) ? actorSupported_yes
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

    public Task<Void> moved(RemoteReference remoteReference, NodeAddress oldAddress, NodeAddress newAddress)
    {
        localAddressCache.put(remoteReference, newAddress);
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
        if (logger.isDebugEnabled())
        {
            logger.debug("Cluster view changed " + nodes);
        }
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
                nodeInfo.nodeCapabilities = stage.getRemoteObserverReference(a, NodeCapabilities.class, "");
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

    public Task<NodeAddress> locateActor(final RemoteReference reference, final boolean forceActivation)
    {
        NodeAddress address = RemoteReference.getAddress(reference);
        if (address != null)
        {
            // TODO: call the node to check.
            return activeNodes.containsKey(address) ? Task.fromValue(address) : Task.fromValue(null);
        }
        return (forceActivation) ? locateAndActivateActor(reference) : locateActiveActor(reference);
    }


    private Task<NodeAddress> locateActiveActor(final RemoteReference<?> actorReference)
    {
        // TODO: change this, it's wrong, it should at least try to locate the actor in the distributed directory
        NodeAddress address = localAddressCache.getIfPresent(actorReference);
        if (address != null && activeNodes.containsKey(address))
        {
            return Task.fromValue(address);
        }
        return Task.fromValue(null);
    }

    private Task<NodeAddress> locateAndActivateActor(final RemoteReference<?> actorReference)
    {
        final RemoteKey remoteKey = new RemoteKey(((RemoteReference) actorReference)._interfaceClass().getName(),
                String.valueOf(((RemoteReference) actorReference).id));

        final Class<?> interfaceClass = ((RemoteReference<?>) actorReference)._interfaceClass();

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
                final String interfaceClassName = interfaceClass.getName();
                return Task.fromValue(selectNode(interfaceClassName, true));
            }
        }

        // Get the existing activation from the local cache (if any)
        NodeAddress address = localAddressCache.getIfPresent(actorReference);

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
            nodeAddress = distributedDirectory.get(remoteKey);
            if (nodeAddress != null)
            {
                // Target node still valid?
                if (activeNodes.containsKey(nodeAddress))
                {
                    // Cache locally
                    localAddressCache.put(actorReference, nodeAddress);
                    return nodeAddress;
                }
                else
                {
                    // Target node is now dead, remove this activation from distributed cache
                    distributedDirectory.remove(remoteKey, nodeAddress);
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
                nodeAddress = selectNode(interfaceClass.getName(), true);
            }

            // Push our selection to the distributed cache (if possible)
            NodeAddress otherNodeAddress = distributedDirectory.putIfAbsent(remoteKey, nodeAddress);

            // Someone else beat us to placement, use that node
            if (otherNodeAddress != null)
            {
                nodeAddress = otherNodeAddress;
            }

            // Add to local cache
            localAddressCache.put(actorReference, nodeAddress);

            return nodeAddress;

        }, stage.getExecutionPool());

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
                nodeType == NodeTypeEnum.SERVER && stage.canActivateActor(interfaceClassName))
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

    @Override
    public Task connect(final HandlerContext ctx, final Object param) throws Exception
    {
        return ctx.connect(param);
    }

    @Override
    public void onActive(final HandlerContext ctx) throws Exception
    {
        stage.registerObserver(NodeCapabilities.class, "", this);
        ctx.fireActive();
    }

    @Override
    public void onRead(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            onInvocation(ctx, (Invocation) msg);
        }
        else
        {
            ctx.fireRead(msg);
        }
    }

    private void onInvocation(final HandlerContext ctx, final Invocation invocation)
    {
        final RemoteReference toReference = invocation.getToReference();
        final NodeAddress localAddress = getNodeAddress();

        if (Objects.equals(toReference.address, localAddress))
        {
            ctx.fireRead(invocation);
            return;
        }
        final NodeAddress cachedAddress = localAddressCache.getIfPresent(toReference);
        if (Objects.equals(cachedAddress, localAddress))
        {
            ctx.fireRead(invocation);
            return;
        }
        if (toReference._interfaceClass().isAnnotationPresent(StatelessWorker.class)
                && stage.canActivateActor(toReference._interfaceClass().getName()))
        {
            // accepting stateless worker call
            ctx.fireRead(invocation);
            return;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Choosing a new node for the invocation");
        }
        locateActor(invocation.getToReference(), true)
                .whenComplete((r, e) -> {
                    if (e != null)
                    {
                        if (invocation.getCompletion() != null)
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Can't find a location for: " + toReference, e);
                            }
                            invocation.getCompletion().completeExceptionally(e);
                        }
                        else
                        {
                            logger.error("Can't find a location for: " + toReference, e);
                        }
                    }
                    else if (Objects.equals(r, localAddress))
                    {
                        // accepts the message
                        ctx.fireRead(invocation);
                    }
                    else if (r != null)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Choosing a remote node for the invocation");
                        }
                        // forwards the message to somewhere else.
                        invocation.setHops(invocation.getHops() + 1);
                        invocation.setToNode(r);
                        ctx.write(invocation);
                    }
                    else
                    {
                        // don't know what to do with it...
                        if (logger.isErrorEnabled())
                        {
                            logger.error("Can't find a destination for {}", invocation);
                        }
                    }
                });
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            final Invocation invocation = (Invocation) msg;
            if (invocation.getToNode() == null)
            {
                return writeInvocation(ctx, invocation);
            }
        }
        return ctx.write(msg);
    }

    protected Task<?> writeInvocation(final HandlerContext ctx, Invocation invocation)
    {
        final Method method = invocation.getMethod();
        final RemoteReference<?> toReference = invocation.getToReference();
        if (method != null && method.isAnnotationPresent(OnlyIfActivated.class))
        {
            if (!await(verifyActivated(toReference, method)))
            {
                return Task.done();
            }
        }
        final Task<?> task;
        if (invocation.getToNode() == null)
        {
            NodeAddress address;
            if (toReference instanceof RemoteReference
                    && (address = RemoteReference.getAddress((RemoteReference) toReference)) != null)
            {
                invocation.withToNode(address);
                task = ctx.write(invocation);
            }
            else
            {
                // TODO: Ensure that both paths encode exception the same way.
                address = await(locateActor(toReference, true));
                task = ctx.write(invocation.withToNode(address));
            }
        }
        else
        {
            task = ctx.write(invocation);
        }
        return task.whenCompleteAsync((r, e) ->
                {
                    // place holder, just to ensure the completion happens in another thread
                },
                stage.getExecutionPool());
    }

    /**
     * Checks if the method passes an Activated check.
     * Verify passes on either of:
     * - method can run only if activated, and the actor is active
     * - the method is not marked with OnlyIfActivated.
     */
    private Task<Boolean> verifyActivated(RemoteReference<?> toReference, Method method)
    {
        if (method.isAnnotationPresent(OnlyIfActivated.class))
        {
            NodeAddress actorAddress = await(locateActor(toReference, false));
            if (actorAddress == null)
            {
                return Task.fromValue(false);
            }
        }
        return Task.fromValue(true);
    }
}
