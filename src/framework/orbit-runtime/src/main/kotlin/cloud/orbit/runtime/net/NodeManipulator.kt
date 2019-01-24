/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.common.concurrent.atomicSet
import cloud.orbit.core.net.NodeCapabilities
import cloud.orbit.core.net.NodeInfo
import cloud.orbit.core.net.NodeStatus
import java.util.concurrent.atomic.AtomicReference

class NodeManipulator {
    private val nodeRef = AtomicReference<NodeInfo>()
    val nodeInfo: NodeInfo get() = nodeRef.get()

    fun replace(nodeInfo: NodeInfo) {
        nodeRef.set(nodeInfo)
    }

    fun updateNodeStatus(newValue: NodeStatus) {
        nodeRef.atomicSet {
            it.copy(nodeStatus = newValue)
        }
    }

    fun updateNodeStatus(expected: NodeStatus, target: NodeStatus) {
        nodeRef.atomicSet {
            if (it.nodeStatus != expected) {
                throw IllegalStateException("Can not transition to $target. Expected $expected but was ${it.nodeStatus}.")
            }
            it.copy(
                nodeStatus = target
            )
        }
    }

    fun updateCapabiltities(nodeCapabilities: NodeCapabilities) {
        nodeRef.atomicSet {
            it.copy(
                nodeCapabilities = nodeCapabilities
            )
        }
    }
}