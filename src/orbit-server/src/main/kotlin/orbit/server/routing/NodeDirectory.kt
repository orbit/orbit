/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.*
import orbit.server.net.NodeId

internal interface NodeDirectory {
    fun connectNode(node: MeshNode, parent: NodeId? = null)
    fun lookupConnectedNodes(nodeId: NodeId, address: Address? = null): Sequence<NodeInfo>
//    fun getNode(nodeId: NodeId): MeshNode?
//    fun reportConnections(nodeId: NodeId, connections: List<NodeInfo>)

    data class NodeInfo(val id: NodeId, val parent: NodeId, val host: String? = null, val port: Int? = null)

    fun lookupMeshNodes(): Sequence<NodeInfo>
    fun getNode(nodeId: NodeId): NodeInfo?
}