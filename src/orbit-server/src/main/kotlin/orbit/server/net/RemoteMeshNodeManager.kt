/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.shared.mesh.NodeId
import java.util.concurrent.ConcurrentHashMap

class RemoteMeshNodeManager(
    private val localNode: LocalNodeInfo,
    private val clusterManager: ClusterManager
) {
    private val connections = ConcurrentHashMap<NodeId, RemoteMeshNodeConnection>()

    suspend fun tick() {
        refreshConnections()
    }

    fun getNode(nodeId: NodeId): RemoteMeshNodeConnection? {
        return connections[nodeId]
    }

    suspend fun refreshConnections() {
        var newConnections = false

        val meshNodes = clusterManager.allNodes
            .filter { node -> node.id.namespace == "management" }
            .filter { node -> !this.connections.containsKey(node.id) && node.id != localNode.info.id }
            .filter { node -> node.hostInfo != null }
        meshNodes.forEach { node ->
            newConnections = true
            this.connections[node.id] = RemoteMeshNodeConnection(localNode, node)
        }

        if (newConnections) {
            clusterManager.updateNode(localNode.info.id) { node ->
                node!!.copy(visibleNodes = node.visibleNodes.plus(meshNodes.map { n -> n.id }))
            }
        }
    }
}
