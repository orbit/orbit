/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.net.NodeId
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo

internal class InMemoryNodeDirectory : NodeDirectory {

    companion object Singleton {
        @JvmStatic
        private var nodes: HashMap<NodeId, NodeInfo> = hashMapOf()
    }

    override fun lookupConnectedNodes(nodeId: NodeId): Sequence<NodeInfo> {
        return nodes.values.filter { node -> node.visibleNodes.contains(nodeId) }.plus(
            nodes[nodeId]?.visibleNodes?.map { node -> nodes[node] } ?: listOf()
        ).filterNotNull().asSequence()
    }

    override fun lookupMeshNodes(): List<NodeInfo.ServerNodeInfo> {
        return nodes.values.filterIsInstance<NodeInfo.ServerNodeInfo>()
    }

    override suspend fun report(node: NodeInfo) {
        nodes[node.id] = node
    }

    suspend override fun join(nodeInfo: NodeInfo) {
        nodes[nodeInfo.id] = nodeInfo
    }

    override fun removeNode(nodeId: NodeId) {
        nodes.remove(nodeId)
    }
}
