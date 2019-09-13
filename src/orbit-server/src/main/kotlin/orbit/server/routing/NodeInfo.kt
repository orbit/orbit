/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.net.NodeId

sealed class NodeInfo {
    abstract val id: NodeId
//        abstract val capabilities: NodeCapabilities
    abstract val visibleNodes: Iterable<NodeId>

    data class LocalServerNodeInfo(
        override val id: NodeId = NodeId.generate("mesh:"),
//            override val capabilities: NodeCapabilities,
        override val visibleNodes: Iterable<NodeId> = ArrayList(),
        val host: String,
        val port: Int
    ) : NodeInfo()

    data class ServerNodeInfo(
        override val id: NodeId,
//            override val capabilities: NodeCapabilities,
        override val visibleNodes: Iterable<NodeId> = ArrayList(),
        val host: String,
        val port: Int
    ) : NodeInfo()

    data class ClientNodeInfo(
        override val id: NodeId,
//            override val capabilities: NodeCapabilities,
        override val visibleNodes: Iterable<NodeId>
    ) : NodeInfo()
}