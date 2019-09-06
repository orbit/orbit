/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.net.NodeId

internal interface NodeDirectory {
    fun connectNode(nodeInfo: NodeInfo)
    fun lookupConnectedNodes(nodeId: NodeId): Sequence<NodeInfo>
    fun report(nodeId: NodeId, visibleNodes: Iterable<NodeId>)

    fun lookupMeshNodes(): List<NodeInfo.ServerNodeInfo>
    fun removeNode(nodeId: NodeId)

    sealed class NodeInfo {
        abstract val id: NodeId
//        abstract val capabilities: NodeCapabilities
        abstract var visibleNodes: Iterable<NodeId>

        data class ServerNodeInfo(
            override val id: NodeId,
//            override val capabilities: NodeCapabilities,
            override var visibleNodes: Iterable<NodeId> = ArrayList(),
            val host: String,
            val port: Int
        ) : NodeInfo()

        data class ClientNodeInfo(
            override val id: NodeId,
//            override val capabilities: NodeCapabilities,
            override var visibleNodes: Iterable<NodeId>
        ) : NodeInfo()
    }}
