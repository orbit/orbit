/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.auth

import orbit.server.mesh.MANAGEMENT_NAMESPACE
import orbit.shared.mesh.NodeId

data class AuthInfo(
    val isManagementNode: Boolean,
    val nodeId: NodeId
)

class AuthSystem {
    suspend fun attemptAuth(nodeId: NodeId): AuthInfo? {
        val isManagement = nodeId.namespace == MANAGEMENT_NAMESPACE
        return AuthInfo(
            isManagementNode = isManagement,
            nodeId = nodeId
        )
    }
}