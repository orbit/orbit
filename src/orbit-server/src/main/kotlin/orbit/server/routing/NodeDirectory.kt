/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.*
import orbit.server.net.NodeId

internal interface NodeDirectory {
    fun connectNode(nodeInfo: NodeInfo)
    fun lookupConnectedNodes(nodeId: NodeId, address: Address? = null): Sequence<NodeInfo>
//    fun reportConnections(nodeId: NodeId, connections: List<NodeInfo>)

    fun lookupMeshNodes(): List<NodeInfo>

    data class NodeInfo(val id: NodeId, val parent: NodeId? = Mesh.NodeId, val host: String? = null, val port: Int? = null)
}
