/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.router

import orbit.shared.mesh.NodeId

data class Route(private val path: List<NodeId> = emptyList()) {
    fun push(nodeId: NodeId): Route {
        return Route(listOf(nodeId).plus(this.path))
    }

    fun pop(): PopResult {
        return PopResult(Route(this.path.drop(1)), this.path.last())
    }

    val nextNode: NodeId
        get() = this.path.first()

    val destinationNode: NodeId
        get() = this.path.last()

    data class PopResult(val route: Route, val nodeId: NodeId) {
    }
}