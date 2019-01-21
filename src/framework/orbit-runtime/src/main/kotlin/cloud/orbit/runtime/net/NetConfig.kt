/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.common.util.RandomUtils
import cloud.orbit.core.net.ClusterName
import cloud.orbit.core.net.NodeIdentity

data class NetConfig(
    /**
     * The [ClusterName] of the Orbit cluster.
     *
     * This value determines which nodes may communicate with one another.
     */
    val clusterName: ClusterName = ClusterName("orbit-cluster"),

    /**
     * The [NodeIdentity] of this Orbit node.
     *
     * This value must be unique.
     */
    val nodeIdentity: NodeIdentity = NodeIdentity(RandomUtils.secureRandomString())
)