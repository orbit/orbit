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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.OnlyIfActivated;
import cloud.orbit.actors.annotation.PreferLocalPlacement;
import cloud.orbit.actors.annotation.StatelessWorker;
import cloud.orbit.actors.cluster.ClusterPeer;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.exceptions.ObserverNotFound;
import cloud.orbit.actors.extensions.NodeSelectorExtension;
import cloud.orbit.actors.extensions.PipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import cloud.orbit.lifecycle.Startable;
import cloud.orbit.util.AnnotationCache;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.ea.async.Async.await;

public class Hosting implements NodeCapabilities, Startable, PipelineExtension
{
    private Logger logger = LoggerFactory.getLogger(Hosting.class);
    private NodeTypeEnum nodeType;
    private ClusterPeer clusterPeer;

    private volatile Map<NodeAddress, NodeInfo> activeNodes = new HashMap<>(0);
    private volatile List<NodeInfo> serverNodes = new ArrayList<>(0);
    private final Object serverNodesUpdateMutex = new Object();
    private Stage stage;

    // according to the micro benchmarks, a guava cache is much slower than using a ConcurrentHashMap here.
    private final Map<RemoteReference<?>, NodeAddress> localAddressCache = new ConcurrentHashMap<>();

    // don't use RemoteReferences, better to restrict keys to a small set of classes.
    private volatile ConcurrentMap<RemoteKey, NodeAddress> distributedDirectory;

    private long timeToWaitForServersMillis = 30000;

    private TreeMap<String, NodeInfo> consistentHashNodeTree = new TreeMap<>();
    private final AnnotationCache<OnlyIfActivated> onlyIfActivateCache = new AnnotationCache<>(OnlyIfActivated.class);

    private CompletableFuture<Void> hostingActive = new Task<>();

    private int maxLocalAddressCacheCount = 10_000;

    private NodeSelectorExtension nodeSelector;

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

    public void setNodeSelector(NodeSelectorExtension nodeSelector)
    {
        this.nodeSelector = nodeSelector;
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
        for (final NodeInfo s : serverNodes)
        {
            set.add(s.address);
        }
        return Collections.unmodifiableList(set);
    }

    public Task<?> notifyStateChange()
    {
        return Task.allOf(activeNodes.values().stream()
                .filter(nodeInfo -> !nodeInfo.address.equals(clusterPeer.localAddress()) && nodeInfo.state == NodeState.RUNNING)
                .map(info -> info.nodeCapabilities.nodeModeChanged(clusterPeer.localAddress(), stage.getState()).exceptionally(throwable -> null)));
    }

    public NodeAddress getNodeAddress()
    {
        return clusterPeer.localAddress();
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
        if (logger.isDebugEnabled())
        {
            logger.debug("Node state changed to be: {}.", newState);
        }
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

    @Override
    public Task<Void> moved(RemoteReference remoteReference, NodeAddress oldAddress, NodeAddress newAddress)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Move {} to from {} to {}.", remoteReference, oldAddress, newAddress);
        }
        setCachedAddress(remoteReference, newAddress);
        return Task.done();
    }

    @Override
    public Task<Void> remove(final RemoteReference<?> remoteReference)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Remove {} from this node.", remoteReference);
        }
        localAddressCache.remove(remoteReference);
        return Task.done();
    }

    public void setClusterPeer(final ClusterPeer clusterPeer)
    {
        this.clusterPeer = clusterPeer;
    }

    @Override
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

        final HashMap<NodeAddress, NodeInfo> oldNodes = new HashMap<>(activeNodes);
        final HashMap<NodeAddress, NodeInfo> newNodes = new HashMap<>(nodes.size());

        final TreeMap<String, NodeInfo> newHashes = new TreeMap<>();

        for (final NodeAddress a : nodes)
        {
            NodeInfo nodeInfo = oldNodes.remove(a);
            if (nodeInfo == null)
            {
                nodeInfo = new NodeInfo(a);
                nodeInfo.nodeCapabilities = stage.getRemoteObserverReference(a, NodeCapabilities.class, "");
                nodeInfo.active = true;
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
        for (final NodeInfo oldNodeInfo : oldNodes.values())
        {
            oldNodeInfo.active = false;
        }
        activeNodes = newNodes;
        consistentHashNodeTree = newHashes;
        updateServerNodes();

        for (NodeInfo info : oldNodes.values())
        {
            // clear local cache
            localAddressCache.values().remove(info.address);

            // TODO notify someone?
        }
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
        final NodeAddress address = RemoteReference.getAddress(reference);
        if (address != null)
        {
            // don't need to call the node call the node to check.
            // checks should be explicit.
            return activeNodes.containsKey(address) ? Task.fromValue(address) : Task.fromValue(null);
        }
        return (forceActivation) ? locateAndActivateActor(reference) : locateActiveActor(reference);
    }


    private Task<NodeAddress> locateActiveActor(final RemoteReference<?> actorReference)
    {
        final NodeAddress address = localAddressCache.get(actorReference);
        if (address != null && activeNodes.containsKey(address))
        {
            return Task.fromValue(address);
        }
        // try to locate the actor in the distributed directory
        // this can be expensive, less that activating the actor, though
        return Task.fromValue(getDistributedDirectory().get(createRemoteKey(actorReference)));
    }

    public void actorDeactivated(RemoteReference remoteReference)
    {
        // removing the reference from the cluster directory and local caches
        getDistributedDirectory().remove(createRemoteKey(remoteReference), clusterPeer.localAddress());
        localAddressCache.remove(remoteReference);
        for (final NodeInfo info : activeNodes.values())
        {
            if (!info.address.equals(clusterPeer.localAddress()) && info.state == NodeState.RUNNING)
            {
                info.nodeCapabilities.remove(remoteReference);
            }
        }
    }

    private Task<NodeAddress> locateAndActivateActor(final RemoteReference<?> actorReference)
    {
        final RemoteKey remoteKey = createRemoteKey(actorReference);

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
                return selectNode(interfaceClass.getName());
            }
        }

        // Get the existing activation from the local cache (if any)
        final NodeAddress address = localAddressCache.get(actorReference);

        // Is this actor already activated and in the local cache? If so, we're done
        if (address != null && activeNodes.containsKey(address))
        {
            return Task.fromValue(address);
        }

        // Get the distributed cache if needed
        final ConcurrentMap<RemoteKey, NodeAddress> distributedDirectory = getDistributedDirectory();

        // Get the existing activation from the distributed cache (if any)
        NodeAddress nodeAddress = distributedDirectory.get(remoteKey);
        if (nodeAddress != null)
        {
            // Target node still valid?
            if (activeNodes.containsKey(nodeAddress))
            {
                return Task.fromValue(nodeAddress);
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
            nodeAddress = await(selectNode(interfaceClass.getName()));
        }

        // Push our selection to the distributed cache (if possible)
        NodeAddress otherNodeAddress = distributedDirectory.putIfAbsent(remoteKey, nodeAddress);

        // Someone else beat us to placement, use that node
        if (otherNodeAddress != null)
        {
            nodeAddress = otherNodeAddress;
        }

        // Cache locally
        setCachedAddress(actorReference, nodeAddress);

        return Task.fromValue(nodeAddress);
    }

    private void setCachedAddress(final RemoteReference<?> actorReference, final NodeAddress nodeAddress)
    {
        cleanup();
        localAddressCache.put(actorReference, nodeAddress);
    }

    private ConcurrentMap<RemoteKey, NodeAddress> getDistributedDirectory()
    {
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
        return distributedDirectory;
    }

    private RemoteKey createRemoteKey(final RemoteReference actorReference)
    {
        return new RemoteKey(actorReference._interfaceClass().getName(),
                String.valueOf(actorReference.id));
    }

    private Task<NodeAddress> selectNode(final String interfaceClassName)
    {
        final long start = System.currentTimeMillis();

        return Task.supplyAsync(() -> {
            while (true)
            {
                if (System.currentTimeMillis() - start > timeToWaitForServersMillis)
                {
                    final String timeoutMessage = "Timeout waiting for a server capable of handling: " + interfaceClassName;
                    logger.error(timeoutMessage);
                    return Task.fromException(new UncheckedException(timeoutMessage));
                }

                // read volatile field into local field
                final List<NodeInfo> currentServerNodes = serverNodes;

                // filter out nodes which cannot host actors (nodes which are not running and nodes which cannot host this actor)
                final List<NodeInfo> potentialNodes = currentServerNodes.stream()
                        .filter(n -> (!n.cannotHostActors && n.state == NodeState.RUNNING)
                                && actorSupported_no != n.canActivate.getOrDefault(interfaceClassName, actorSupported_yes))
                        .collect(Collectors.toList());

                if (potentialNodes.isEmpty())
                {
                    // fail if there are no nodes in the cluster and this nodes state is not running
                    if (stage.getState() != NodeCapabilities.NodeState.RUNNING)
                    {
                        return Task.fromException(new UncheckedException("No node found to activate " + interfaceClassName));
                    }
                    // "spin" and sleep every second to avoid burning cpu cycles, a node joining the cluster will interrupt the sleep
                    if (logger.isDebugEnabled())
                    {
                        logger.info("No node available to activate actor: {}.", interfaceClassName);
                    }
                    waitForServers();
                }
                else
                {
                    for (NodeInfo potentialNode : potentialNodes)
                    {
                        // loop over the potential nodes, add the interface to a concurrent hashset, if add returns true there is no activation pending
                        if (potentialNode.canActivatePending.add(interfaceClassName))
                        {
                            // query if this node can activate this actor
                            potentialNode.nodeCapabilities.canActivate(interfaceClassName).handle((result, throwable) ->
                            {
                                if (throwable != null)
                                {
                                    // actor call to can activate failed, retry next loop
                                    potentialNode.canActivatePending.remove(interfaceClassName);
                                }
                                else
                                {
                                    // node cannot host this actor, mark it so
                                    if (result == actorSupported_noneSupported)
                                    {
                                        potentialNode.cannotHostActors = true;
                                        // jic
                                        potentialNode.canActivate.put(interfaceClassName, actorSupported_no);
                                    }
                                    else
                                    {
                                        // found suitable host
                                        potentialNode.canActivate.put(interfaceClassName, result);
                                    }
                                }
                                return null;
                            });
                        }
                    }

                    final List<NodeInfo> suitableNodes = potentialNodes.stream()
                            .filter(n -> actorSupported_no != n.canActivate.getOrDefault(interfaceClassName, actorSupported_no))
                            .collect(Collectors.toList());

                    if (!suitableNodes.isEmpty())
                    {
                        final NodeInfo nodeInfo = nodeSelector.select(interfaceClassName, getNodeAddress(), suitableNodes);
                        return Task.fromValue(nodeInfo.address);
                    }
                }
            }
        }, stage.getExecutionPool());
    }

    private void waitForServers()
    {
        synchronized (serverNodesUpdateMutex)
        {
            try
            {
                serverNodesUpdateMutex.wait(250);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
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
            if (ThreadLocalRandom.current().nextInt(100) < percentile)
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
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(key.getBytes("UTF-8"));
            final byte[] digest = md.digest();
            return String.format("%064x", new java.math.BigInteger(1, digest));
        }
        catch (final Exception e)
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
        hostingActive.complete(null);
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

    private Task onInvocation(final HandlerContext ctx, final Invocation invocation)
    {
        final RemoteReference toReference = invocation.getToReference();
        final NodeAddress localAddress = getNodeAddress();

        if (Objects.equals(toReference.address, localAddress))
        {
            ctx.fireRead(invocation);
            return Task.done();
        }
        final NodeAddress cachedAddress = localAddressCache.get(toReference);
        if (Objects.equals(cachedAddress, localAddress))
        {
            ctx.fireRead(invocation);
            return Task.done();
        }
        if (toReference._interfaceClass().isAnnotationPresent(StatelessWorker.class)
                && stage.canActivateActor(toReference._interfaceClass().getName()))
        {
            // accepting stateless worker call
            ctx.fireRead(invocation);
            return Task.done();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Choosing a new node for the invocation");
        }

        // over here the actor address is not the localAddress.
        // since we received this message someone thinks that this node is the right one.
        // so we remove that entry from the local cache and query the global cache again
        localAddressCache.remove(toReference);
        return locateActor(invocation.getToReference(), true)
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
                        final NodeInfo info = activeNodes.get(invocation.getFromNode());
                        if (info != null && info.state == NodeState.RUNNING)
                        {
                            try
                            {
                                info.nodeCapabilities.moved(toReference, localAddress, r);
                            }
                            catch (final RuntimeException ignore)
                            {
                                logger.error("Got exception when trying to move an actor.", ignore);
                            }
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
                            logger.error("Failed to find destination for {}", invocation);
                        }
                    }
                });
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Invocation)
        {
            // await checks isDone()
            await(hostingActive);
            final Invocation invocation = (Invocation) msg;
            if (invocation.getFromNode() == null)
            {
                // used by subsequent filters
                invocation.setFromNode(stage.getLocalAddress());
            }
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
        if (onlyIfActivateCache.isAnnotated(method))
        {
            if (!await(verifyActivated(toReference)))
            {
                return Task.done();
            }
        }
        final Task<?> task;
        if (invocation.getToNode() == null)
        {
            NodeAddress address;
            if ((address = RemoteReference.getAddress(toReference)) != null)
            {
                invocation.withToNode(address);
                if (!activeNodes.containsKey(address))
                {
                    return Task.fromException(new ObserverNotFound("Node no longer active"));
                }
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
     * Checks if the object is activated.
     */
    private Task<Boolean> verifyActivated(RemoteReference<?> toReference)
    {
        final NodeAddress actorAddress = await(locateActor(toReference, false));
        return Task.fromValue(actorAddress != null);
    }

    public void cleanup()
    {
        if (localAddressCache.size() > maxLocalAddressCacheCount)
        {
            // randomly removes local references
            final List<RemoteReference<?>> remoteReferences = new ArrayList<>(localAddressCache.keySet());
            Collections.shuffle(remoteReferences);
            for (int c = remoteReferences.size() - maxLocalAddressCacheCount / 2; --c >= 0; )
            {
                localAddressCache.remove(remoteReferences.get(c));
            }
        }
    }

    public int getMaxLocalAddressCacheCount()
    {
        return maxLocalAddressCacheCount;
    }

    public void setMaxLocalAddressCacheCount(final int maxLocalAddressCacheCount)
    {
        this.maxLocalAddressCacheCount = maxLocalAddressCacheCount;
    }
}
