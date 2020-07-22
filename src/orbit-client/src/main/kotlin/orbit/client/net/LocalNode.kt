/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import orbit.client.OrbitClientConfig
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeInfo
import orbit.util.concurrent.atomicSet
import java.util.concurrent.atomic.AtomicReference

internal class LocalNode(private val config: OrbitClientConfig) {
    private val default get() = NodeData(config.grpcEndpoint, config.namespace)

    private val ref = AtomicReference(
        default.copy()
    )

    val status get() = ref.get()!!

    fun manipulate(body: (NodeData) -> NodeData) = ref.atomicSet(body)!!

    fun reset() {
        manipulate {
            default.copy()
        }
    }
}

internal data class NodeData(
    val grpcEndpoint: String,
    val namespace: String,
    val nodeInfo: NodeInfo? = null,
    val capabilities: NodeCapabilities? = null,
    val clientState: ClientState = ClientState.IDLE
)