/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory

internal class OutgoingConnections(val localNodeId: LocalNodeId, val nodeDirectory: NodeDirectory) :
    ConnectionHost {

    override fun getNode(nodeId: NodeId): MeshNode? {
        return activeNodes[nodeId]
    }

    override fun getActiveNodes(): List<MeshNode> {
        return activeNodes.values.toList()
    }

    private val activeNodes = hashMapOf<NodeId, MeshNode>()

    fun refreshConnections() {
        val meshNodes = nodeDirectory.lookupMeshNodes().toList()

        meshNodes.filter { node -> !activeNodes.containsKey(node.id) && node.id != localNodeId.nodeId }.forEach { node ->
            val client = GrpcMeshNodeClient(node.id, node.host, node.port)

            activeNodes[node.id] = client
        }

        nodeDirectory.report(localNodeId.nodeId, getActiveNodes().map { node -> node.id })
    }
}
