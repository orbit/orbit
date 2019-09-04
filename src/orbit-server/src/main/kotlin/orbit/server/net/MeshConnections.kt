/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import kotlin.concurrent.fixedRateTimer

internal class MeshConnections(val nodeDirectory: NodeDirectory) :
    ConnectionHost {

    override fun getNode(nodeId: NodeId): MeshNode? {
        return activeNodes[nodeId]
    }

    override fun getActiveNodes(): List<MeshNode> {
        return activeNodes.values.toList()
    }

    private val activeNodes = hashMapOf<NodeId, MeshNode>()

    fun start() {
        fixedRateTimer(name = "mesh connections", daemon = true, initialDelay = 0L, period = 10000) {
            connect()
        }
    }

    fun connect() {
        val meshNodes = nodeDirectory.lookupMeshNodes().toList()

        val unconnected = meshNodes.filter { node -> !activeNodes.containsKey(node.id) }

        unconnected.filter { node -> !node.host.isNullOrEmpty() && node.port != null }.forEach { node ->
            val client = GrpcMeshNodeClient(node.id, node.host!!, node.port!!)

            activeNodes[node.id] = client
        }
    }
}
