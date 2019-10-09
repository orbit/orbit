/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import orbit.client.OrbitClientConfig
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeInfo
import orbit.util.concurrent.jvm.atomicSet
import java.util.concurrent.atomic.AtomicReference

class LocalNode(config: OrbitClientConfig) {
    private val ref = AtomicReference(
        NodeStatus(config.serviceLocator)
    )

    val status get() = ref.get()!!

    fun manipulate(body: (NodeStatus) -> NodeStatus) = ref.atomicSet(body)!!
}

data class NodeStatus(
    val serviceLocator: OrbitServiceLocator,
    val nodeInfo: NodeInfo? = null,
    val capabilities: NodeCapabilities? = null
)