/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.hosting

import cloud.orbit.common.util.randomOrNull
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.net.NodeInfo

/**
 * A routing strategy determines how a message is routed based on a set of rules.
 */
interface RoutingStrategy {
    /**
     * Selects a target.
     *
     * @param nodes The nodes to choose from.
     * @return A target or null if no suitable node found.
     */
    fun selectTarget(nodes: List<NodeInfo>): NetTarget?
}

/**
 * A routing strategy that selects a random single node as its target.
 */
class RandomRouting : RoutingStrategy {
    override fun selectTarget(nodes: List<NodeInfo>): NetTarget? = nodes.randomOrNull()?.nodeIdentity?.asTarget()
}