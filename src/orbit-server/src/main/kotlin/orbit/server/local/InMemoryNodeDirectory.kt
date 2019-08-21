/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.*
import orbit.server.routing.*

class InMemoryNodeDirectory : NodeDirectory {
    private val nodes = hashMapOf<NodeId, MeshNode>(Mesh.Instance.id to Mesh.Instance)
    private val connections = ArrayList<Connection>()

    override fun getNode(nodeId: NodeId): MeshNode? {
        return this.nodes[nodeId]
    }

    override fun lookupConnectedNodes(nodeId: NodeId, address: BaseAddress): Sequence<MeshNode> {
        val searchNode = nodes[nodeId] ?: return emptySequence()
        return sequence {
            if (searchNode.canHandle(address)) {
                yieldAll(connections.filter { c -> c.parent.equals(nodeId) }.mapNotNull { c -> nodes[c.node] })
                yieldAll(connections.filter { c -> c.node.equals(nodeId) }.mapNotNull { c -> nodes[c.parent] })
            }
        }
    }

    fun connectNode(node: MeshNode, parent: NodeId? = null) {
        nodes[node.id] = node
        connections.add(Connection(node.id, parent ?: Mesh.Instance.id))
    }

    data class Connection(val node: NodeId, val parent: NodeId)
}
