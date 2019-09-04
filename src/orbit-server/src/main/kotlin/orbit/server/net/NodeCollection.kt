/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.server.routing.MeshNode

internal class NodeCollection(
    private val meshConnections: MeshConnections,
    private val clientConnections: ClientConnections
) {
    fun getNode(nodeId: NodeId): MeshNode? {
        return meshConnections.getNode(nodeId) ?: clientConnections.getNode(nodeId)
    }

}