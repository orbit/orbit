/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo

internal class OutgoingConnections(val localNode: NodeInfo.LocalServerNodeInfo, val nodeDirectory: NodeDirectory) {

    fun getNode(nodeId: NodeId): MeshNode? {
        return activeNodes[nodeId]
    }

    private val activeNodes = hashMapOf<NodeId, MeshNode>()

    suspend fun refreshConnections() {
        val meshNodes = nodeDirectory.lookupMeshNodes().toList()

        meshNodes.filter { node -> !activeNodes.containsKey(node.id) && node.id != localNode.id }
            .forEach { node ->
                val client = GrpcMeshNodeClient(node.id, node.host, node.port)

                activeNodes[node.id] = client
            }

        nodeDirectory.report(NodeInfo.ServerNodeInfo(localNode.id, activeNodes.keys, localNode.host, localNode.port) )
    }
}
