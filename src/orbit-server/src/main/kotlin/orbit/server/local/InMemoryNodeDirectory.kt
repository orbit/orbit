/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.*
import orbit.server.net.NodeId
import orbit.server.routing.*

internal class InMemoryNodeDirectory : NodeDirectory {
    private val nodes = hashMapOf<NodeId, MeshNode>(Mesh.Instance.id to Mesh.Instance)
    private var connections: List<Connection> = listOf()

    override fun getNode(nodeId: NodeId): MeshNode? {
        return this.nodes[nodeId]
    }

    override fun lookupConnectedNodes(nodeId: NodeId, address: Address): Sequence<MeshNode> {
        val searchNode = nodes[nodeId] ?: return emptySequence()
        return sequence {
            if (searchNode.canHandle(address)) {
                yieldAll(connections.filter { c -> c.parent.equals(nodeId) }.mapNotNull { c -> nodes[c.node] })
                yieldAll(connections.filter { c -> c.node.equals(nodeId) }.mapNotNull { c -> nodes[c.parent] })
            }
        }
    }

    override fun reportConnections(nodeId: NodeId, connections: List<NodeId>) {
        this.connections = this.connections.filter { c -> (c.parent != nodeId && c.node != nodeId) }
            .plus(connections.map { c -> Connection(nodeId, c) })
    }

    override fun connectNode(node: MeshNode, parent: NodeId?) {
        nodes[node.id] = node
        connections = connections.plus(Connection(node.id, parent ?: Mesh.Instance.id))
    }

    data class Connection(val node: NodeId, val parent: NodeId)
}
