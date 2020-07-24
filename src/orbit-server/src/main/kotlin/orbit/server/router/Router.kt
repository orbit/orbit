/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.router

import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.shared.mesh.NodeId
import orbit.shared.router.Route

@Suppress("UnstableApiUsage")
class Router(private val localNode: LocalNodeInfo, private val clusterManager: ClusterManager) {
    @Suppress("UNUSED_PARAMETER")
    fun findRoute(targetNode: NodeId, possibleRoute: Route? = null): Route {
        val path = clusterManager.findRoute(localNode.info.id, targetNode)

        return Route(path)
    }
}




