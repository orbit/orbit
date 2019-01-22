/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.common.concurrent.atomicSet
import cloud.orbit.core.net.ClusterInfo
import cloud.orbit.core.net.NodeInfo
import cloud.orbit.core.net.NodeStatus
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReference

class NetManager {
    @PublishedApi
    internal val localNodeRef = AtomicReference<NodeInfo>()
    @PublishedApi
    internal val localClusterInfoRef = AtomicReference<ClusterInfo>()

    val localNode: NodeInfo get() = localNodeRef.get()
    val localClusterInfo: ClusterInfo get() = localClusterInfoRef.get()

    fun updateLocalNodeStatus(expected: NodeStatus, target: NodeStatus) {
        localNodeRef.atomicSet {
            if(it.nodeStatus != expected) {
                throw IllegalStateException("Can not transition to $target. Expected $expected but was ${it.nodeStatus}.")
            }
            it.copy(
                nodeStatus = target
            )
        }
    }
}