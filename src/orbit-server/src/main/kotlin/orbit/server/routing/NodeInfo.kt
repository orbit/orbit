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
    abstract val id: NodeId
    abstract val capabilities: NodeCapabilities
    abstract val visibleNodes: Iterable<NodeId>

    abstract val lease: NodeLease

    data class ServerNodeInfo(
        override val id: NodeId = NodeId.Empty,
        override val capabilities: NodeCapabilities = NodeCapabilities(),
        override val visibleNodes: Iterable<NodeId> = ArrayList(),
        override val lease: NodeLease = NodeLease.Empty,
        val host: String,
        val port: Int
    ) : NodeInfo()

    data class ClientNodeInfo(
        override val id: NodeId = NodeId.Empty,
        override val capabilities: NodeCapabilities = NodeCapabilities(),
        override val lease: NodeLease = NodeLease.Empty,
        override val visibleNodes: Iterable<NodeId> = ArrayList()
    ) : NodeInfo()
}

data class NodeCapabilities(
    val addressableTypes: Iterable<AddressableType>
) {
    constructor(vararg addressableTypes: AddressableType) : this(addressableTypes.asIterable())
    constructor() : this(listOf())
}