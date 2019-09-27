/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.addressable.AddressableType
import orbit.server.net.NodeId
import orbit.server.net.NodeLease

sealed class NodeInfo {
    companion object {}

    abstract val id: NodeId
    abstract val capabilities: NodeCapabilities
    abstract val visibleNodes: Iterable<NodeId>

    abstract val lease: NodeLease

    fun clone(
        id: NodeId = NodeId.Empty,
        capabilities: NodeCapabilities = NodeCapabilities(),
        visibleNodes: Set<NodeId> = HashSet(),
        lease: NodeLease = NodeLease.Empty
    ): NodeInfo {
        return when (this) {
            is ServerNodeInfo -> this.copy(id, capabilities, visibleNodes, lease)
            is ClientNodeInfo -> this.copy(id, capabilities, visibleNodes, lease)
        }
    }

    data class ServerNodeInfo(
        override val id: NodeId = NodeId.Empty,
        override val capabilities: NodeCapabilities = NodeCapabilities(),
        override val visibleNodes: Set<NodeId> = HashSet(),
        override val lease: NodeLease = NodeLease.Empty,
        val host: String,
        val port: Int
    ) : NodeInfo()

    data class ClientNodeInfo(
        override val id: NodeId = NodeId.Empty,
        override val capabilities: NodeCapabilities = NodeCapabilities(),
        override val visibleNodes: Set<NodeId> = HashSet(),
        override val lease: NodeLease = NodeLease.Empty
    ) : NodeInfo()
}

data class NodeCapabilities(
    val addressableTypes: Iterable<AddressableType>
) {
    constructor(vararg addressableTypes: AddressableType) : this(addressableTypes.asIterable())
    constructor() : this(listOf())
}
