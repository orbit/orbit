/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import java.util.concurrent.atomic.AtomicReference

class LocalNodeInfo(initialValue: NodeInfo.ServerNodeInfo) {
    private val nodeRef = AtomicReference(initialValue)

    val nodeInfo: NodeInfo.ServerNodeInfo get() = nodeRef.get()

    fun updateNodeInfo(newNodeInfo: NodeInfo.ServerNodeInfo) {
        nodeRef.set(newNodeInfo)
    }
}