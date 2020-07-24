/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.router

import orbit.shared.mesh.NodeId

data class Route(val path: List<NodeId> = emptyList()) {
    fun pop(): PopResult {
        return PopResult(Route(this.path.drop(1)), this.path.last())
    }

    fun isValid(): Boolean {
        return !this.path.isEmpty()
    }

    val nextNode: NodeId
        get() = this.path.first()

    data class PopResult(val route: Route, val nodeId: NodeId)
}