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
import cloud.orbit.exception.UncheckedException;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.fork.ForkChannel;
import org.jgroups.protocols.FORK;
import org.jgroups.protocols.FRAG2;
import org.jgroups.protocols.UDP;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinTask;

public class JGroupsClusterPeer implements ClusterPeer
{
    private static final Logger logger = LoggerFactory.getLogger(JGroupsClusterPeer.class);

    private int portRangeLength = 1000;
    private Task<Address> startFuture;
    private ForkChannel channel;
    private DefaultCacheManager cacheManager;
    private NodeInfo local;

    private NodeInfo master;
    private volatile Map<Address, NodeInfo> nodeMap = new HashMap<>();
    private volatile Map<NodeAddress, NodeInfo> nodeMap2 = new HashMap<>();
    private ViewListener viewListener;
    private MessageListener messageListener;

    private String jgroupsConfig = "classpath:/conf/jgroups.xml";

    private boolean nameBasedUpdPort = true;

    @Override
    public NodeAddress localAddress()
    {
        sync();
        return local.nodeAddress;
    }

    @Override
    public void registerViewListener(final ViewListener viewListener)
    {
        this.viewListener = viewListener;
    }

    @Override
    public void registerMessageReceiver(final MessageListener messageListener)
    {
        this.messageListener = messageListener;
    }

    protected static class NodeInfo
    {
        private Address address;
        private NodeAddress nodeAddress;

        public NodeInfo(final Address address)
        {
            this.address = address;
            final UUID jgroupsUUID = (UUID) address;
            this.nodeAddress = new NodeAddressImpl(new java.util.UUID(jgroupsUUID.getMostSignificantBits(), jgroupsUUID.getLeastSignificantBits()));
        }
    }

    public Task<?> join(final String clusterName, final String nodeName)
    {
        final ForkJoinTask<Address> f = ForkJoinTask.adapt(new Callable<Address>()
        {
            @Override
            public Address call()
            {
                try
                {
                    if (System.getProperty("java.net.preferIPv4Stack", null) == null)
                    {
                        System.setProperty("java.net.preferIPv4Stack", "true");
                    }
                    // the parameter of this constructor defines the protocol stack
                    // we are using the default that allows discovery based on broadcast packets.
                    // It must be asserted that the production network support (enables) this.
                    // Otherwise it's also possible to change the discovery mechanism.
                    JChannel baseChannel = new JChannel(configToURL(getJgroupsConfig()));
                    baseChannel.setName(nodeName);

                    if (isNameBasedUpdPort() && baseChannel.getProtocolStack().getBottomProtocol() instanceof UDP)
                    {
                        final UDP udp = (UDP) baseChannel.getProtocolStack().getBottomProtocol();
                        udp.setMulticastPort(udp.getMulticastPort() + ((clusterName.hashCode() & 0x8fff_ffff) % portRangeLength));
                    }

                    // TODO: Remove when using JGroups 3.6.7.Final see https://github.com/belaban/JGroups/pull/241
                    synchronized (baseChannel) {
                        ProtocolStack stack = baseChannel.getProtocolStack();
                        FORK fork = (FORK) stack.findProtocol(FORK.class);
                        if (fork == null)
                        {
                            fork = new FORK();
                            fork.setProtocolStack(stack);
                            stack.insertProtocol(fork, ProtocolStack.ABOVE, FRAG2.class);
                        }
                    }

                    channel = new ForkChannel(baseChannel,
                            "hijack-stack",
                            "lead-hijacker",
                            true,
                            ProtocolStack.ABOVE,
                            FRAG2.class);

                    channel.setReceiver(new ReceiverAdapter()
                    {

                        @Override
                        public void viewAccepted(final View view)
                        {
                            doViewAccepted(view);
                        }

                        @Override
                        public void receive(final Message msg)
                        {
                            doReceive(msg);
                        }

                    });

                    final GlobalConfigurationBuilder globalConfigurationBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
                    globalConfigurationBuilder.globalJmxStatistics().allowDuplicateDomains(true);
                    globalConfigurationBuilder.transport().clusterName(clusterName).nodeName(nodeName).transport(new JGroupsTransport(baseChannel));

                    ConfigurationBuilder builder = new ConfigurationBuilder();
                    builder.clustering().cacheMode(CacheMode.DIST_ASYNC);

                    cacheManager = new DefaultCacheManager(globalConfigurationBuilder.build(), builder.build());

                    // need to get a cache, any cache to force the initialization
                    cacheManager.getCache("distributedDirectory");

                    channel.connect(clusterName);
                    local = new NodeInfo(channel.getAddress());
                    logger.info("Registering the local address");
                    logger.info("Done with JGroups initialization");
                    return local.address;
                }
                catch (final Exception e)
                {
                    logger.error("Error during JGroups initialization", e);
                    throw new UncheckedException(e);
                }
            }

        });
        startFuture = Task.fromFuture(f);
        f.fork();
        return startFuture;
    }

    private URL configToURL(final String jgroupsConfig) throws MalformedURLException
    {
        if (jgroupsConfig.startsWith("classpath:"))
        {
            // classpath resource
            final String resourcePath = jgroupsConfig.substring("classpath:".length());
            final URL resource = getClass().getResource(resourcePath);
            if (resource == null)
            {
                throw new IllegalArgumentException("Can't find classpath resource: " + resourcePath);
            }
            return resource;
        }
        if (!jgroupsConfig.contains(":"))
        {
            // normal file
            return Paths.get(jgroupsConfig).toUri().toURL();
        }
        return new URL(jgroupsConfig);
    }

    @Override
    public void leave()
    {
        channel.close();
        channel = null;
        cacheManager.stop();
    }

    // ensures that the channel is connected
    private void sync()
    {
        if (startFuture != null && !startFuture.isDone())
        {
            startFuture.join();
        }
    }

    private void doViewAccepted(final View view)
    {
        final LinkedHashMap<Address, NodeInfo> newNodes = new LinkedHashMap<>(view.size());
        final LinkedHashMap<NodeAddress, NodeInfo> newNodes2 = new LinkedHashMap<>(view.size());
        for (final Address a : view)
        {
            NodeInfo info = nodeMap.get(a);
            if (info == null)
            {
                info = new NodeInfo(a);
            }
            newNodes.put(a, info);
            newNodes2.put(info.nodeAddress, info);
        }
        final NodeInfo newMaster = newNodes.values().iterator().next();
        nodeMap = Collections.unmodifiableMap(newNodes);
        nodeMap2 = Collections.unmodifiableMap(newNodes2);
        master = newMaster;
        viewListener.onViewChange(nodeMap2.keySet());
    }

    @SuppressWarnings("PMD.AvoidThrowingNullPointerException")
    public void sendMessage(NodeAddress address, byte message[])
    {
        sync();
        try
        {
            Objects.requireNonNull(address, "node address");
            final NodeInfo node = nodeMap2.get(address);
            if (node == null)
            {
                throw new IllegalArgumentException("Cluster node not found: " + address);
            }
            ForkChannel channel = this.channel;
            if (channel == null || !channel.isOpen())
            {
                throw new IllegalStateException("Cluster not connected");
            }
            channel.send(node.address, message);
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    @Override
    public <K, V> ConcurrentMap<K, V> getCache(final String name)
    {
        return cacheManager.getCache(name);
    }

    protected void doReceive(final Message msg)
    {
        final NodeInfo nodeInfo = nodeMap.get(msg.getSrc());
        if (nodeInfo == null)
        {
            logger.warn("Received message from invalid address {}", msg.getSrc());
            messageListener.receive(new NodeAddressImpl(new java.util.UUID(((UUID) msg.getSrc()).getMostSignificantBits(), ((UUID) msg.getSrc()).getLeastSignificantBits())), msg.getBuffer());
        }
        else
        {
            messageListener.receive(nodeInfo.nodeAddress, msg.getBuffer());
        }
    }

    public NodeAddress getMaster()
    {
        return master != null ? master.nodeAddress : null;
    }

    public String getJgroupsConfig()
    {
        return jgroupsConfig;
    }

    public void setJgroupsConfig(final String jgroupsConfig)
    {
        this.jgroupsConfig = jgroupsConfig;
    }

    public boolean isNameBasedUpdPort()
    {
        return nameBasedUpdPort;
    }

    public void setNameBasedUpdPort(final boolean nameBasedUpdPort)
    {
        this.nameBasedUpdPort = nameBasedUpdPort;
    }

    public int getPortRangeLength()
    {
        return portRangeLength;
    }

    public void setPortRangeLength(final int portRangeLength)
    {
        this.portRangeLength = portRangeLength;
    }
}
