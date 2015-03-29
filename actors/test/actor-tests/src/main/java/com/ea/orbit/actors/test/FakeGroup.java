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

package com.ea.orbit.actors.test;

import com.ea.orbit.actors.cluster.INodeAddress;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class FakeGroup
{
    private static final Logger logger = LoggerFactory.getLogger(FakeGroup.class);

    private static LoadingCache<String, FakeGroup> groups = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<String, FakeGroup>()
            {
                @Override
                public FakeGroup load(final String key) throws Exception
                {
                    return new FakeGroup(key);
                }
            });

    private Map<INodeAddress, FakeClusterPeer> currentChannels = new HashMap<>();

    private Object topologyMutex = new Object();
    private LoadingCache<String, ConcurrentMap> maps = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, ConcurrentMap>()
            {
                @Override
                public ConcurrentMap load(final String key) throws Exception
                {
                    return new ConcurrentHashMap();
                }
            });
    private int count = 0;
    private String clusterName;
    private static Executor pool = ExecutorUtils.newScalingThreadPool(20);

    public FakeGroup(final String clusterName)
    {
        this.clusterName = clusterName;
    }

    protected NodeAddress join(final FakeClusterPeer fakeChannel)
    {
        Collection<CompletableFuture<?>> tasks;
        NodeAddress nodeAddress;
        synchronized (topologyMutex)
        {
            final String name = "channel." + (++count) + "." + clusterName;
            nodeAddress = new NodeAddress(new UUID(name.hashCode(), count));
            currentChannels.put(nodeAddress, fakeChannel);
            fakeChannel.setAddress(nodeAddress);

            final ArrayList<INodeAddress> newView = new ArrayList<>(currentChannels.keySet());

            tasks = currentChannels.values().stream().map(ch -> CompletableFuture.runAsync(() -> ch.onViewChanged(newView), pool)).collect(Collectors.toList());
        }
        Task.allOf(tasks).join();

        return nodeAddress;
    }

    public void leave(final FakeClusterPeer fakeClusterPeer)
    {
        List<CompletableFuture<?>> tasks;
        NodeAddress nodeAddress;
        synchronized (topologyMutex)
        {
            currentChannels.remove(fakeClusterPeer.localAddress());
            final ArrayList<INodeAddress> newView = new ArrayList<>(currentChannels.keySet());
            tasks = currentChannels.values().stream().map(ch -> CompletableFuture.runAsync(() -> ch.onViewChanged(newView), pool)).collect(Collectors.toList());
        }
        Task.allOf(tasks).join();
    }


    public void sendMessage(final INodeAddress from, final INodeAddress to, final byte[] buff)
    {
        CompletableFuture.runAsync(() -> {
            try
            {
                final FakeClusterPeer fakeClusterPeer = currentChannels.get(to);
                if (fakeClusterPeer == null)
                {
                    throw new UncheckedException("Unknown address: " + to);
                }
                fakeClusterPeer.onMessageReceived(from, buff);
            }
            catch (Exception ex)
            {
                logger.error("Error sending message", ex);
            }
        }, pool);
    }

    public static FakeGroup get(final String clusterName)
    {
        try
        {
            final FakeGroup fakeGroup = groups.get(clusterName);
            return fakeGroup;
        }
        catch (ExecutionException e)
        {
            throw new UncheckedException(e);
        }
    }

    public <K, V> ConcurrentMap<K, V> getCache(final String name)
    {
        try
        {
            return maps.get(name);
        }
        catch (ExecutionException e)
        {
            throw new UncheckedException(e);
        }
    }

    Executor pool()
    {
        return pool;
    }
}
