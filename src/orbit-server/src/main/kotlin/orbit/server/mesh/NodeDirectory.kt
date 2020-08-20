/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.server.service.HealthCheck
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.util.concurrent.AsyncMap

interface NodeDirectory : AsyncMap<NodeId, NodeInfo>, HealthCheck {
    suspend fun entries(): Iterable<Pair<NodeId, NodeInfo>>
    suspend fun tick() {}
}