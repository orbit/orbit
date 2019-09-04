/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.server.routing.MeshNode

internal interface ConnectionHost {
    fun getActiveNodes(): List<MeshNode>
    fun getNode(nodeId: NodeId): MeshNode?
}