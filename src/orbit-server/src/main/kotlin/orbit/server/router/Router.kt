/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.router

import orbit.shared.mesh.NodeId
import orbit.shared.router.Route

class Router {
    @Suppress("UNUSED_PARAMETER")
    fun findRoute(nodeId: NodeId, possibleRoute: Route? = null): Route {
        return Route(listOf(nodeId))
    }
}