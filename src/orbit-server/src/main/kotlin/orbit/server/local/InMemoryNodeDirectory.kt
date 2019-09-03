/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import com.google.common.collect.*
import orbit.server.*
import orbit.server.net.NodeId
import orbit.server.routing.*
import orbit.server.routing.NodeDirectory.NodeInfo

internal class InMemoryNodeDirectory : NodeDirectory {

    companion object Singleton {
        @JvmStatic
        private var nodes: HashMap<NodeId, NodeInfo> = hashMapOf()

        @JvmStatic
        private var activeConnections: BiMap<NodeId, NodeId> = HashBiMap.create()
    }

    private val mesh = Mesh(this)

    override fun lookupConnectedNodes(nodeId: NodeId, address: Address?): Sequence<NodeInfo> {
        return nodes.values.filter { c -> c.parent == nodeId }.plus(
            nodes[nodes[nodeId]?.parent]
        ).filterNotNull().asSequence()
    }

    override fun lookupMeshNodes(): Sequence<NodeInfo> {
        return lookupConnectedNodes(mesh.id)
    }


    override fun getNode(nodeId: NodeId): NodeInfo? {
        return nodes[nodeId]
    }

//    override fun reportConnections(nodeId: NodeId, connections: List<NodeId>) {
//        nodes = nodes.filter { c -> (c.parent != nodeId && c.id != nodeId) }
//            .plus(connections.map { c -> NodeInfo(nodeId, c) })
//    }

    override fun connectNode(node: MeshNode, parent: NodeId?) {
//        nodes[node.id] = node
        nodes[node.id] = NodeInfo(node.id, parent ?: mesh.id)
    }
}
