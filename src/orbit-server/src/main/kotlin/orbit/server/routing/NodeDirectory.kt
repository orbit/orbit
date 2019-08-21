/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.*
import orbit.server.net.NodeId

interface NodeDirectory {
    fun lookupConnectedNodes(nodeId: NodeId, address: Address): Sequence<MeshNode>
    fun getNode(nodeId: NodeId): MeshNode?
    fun reportConnections(nodeId: NodeId, connections: List<NodeId>)
}