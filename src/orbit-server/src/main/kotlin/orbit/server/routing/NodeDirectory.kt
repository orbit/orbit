/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.*

interface NodeDirectory {
    fun lookupConnectedNodes(nodeId: NodeId, address: BaseAddress): Sequence<MeshNode>
    fun getNode(nodeId: NodeId): MeshNode?
}