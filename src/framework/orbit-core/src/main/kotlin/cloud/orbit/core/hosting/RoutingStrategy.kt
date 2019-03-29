/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.hosting

import cloud.orbit.common.util.randomOrNull
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.net.NodeInfo

interface RoutingStrategy {
    fun selectTarget(nodes: List<NodeInfo>): NetTarget?
}

class RandomRouting : RoutingStrategy {
    override fun selectTarget(nodes: List<NodeInfo>): NetTarget? {
        val node = nodes.randomOrNull()
        return if (node != null) {
            NetTarget.Unicast(node.nodeIdentity)
        } else {
            null
        }
    }
}