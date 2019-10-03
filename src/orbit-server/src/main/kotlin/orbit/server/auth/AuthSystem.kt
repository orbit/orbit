/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.auth

import orbit.server.mesh.MANAGEMENT_NAMESPACE
import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId

data class AuthInfo(
    val isManagementNode: Boolean,
    val namespace: Namespace,
    val nodeId: NodeId
)

class AuthSystem {
    suspend fun attemptAuth(namespace: Namespace, nodeId: NodeId): AuthInfo? {
        val isManagement = namespace == MANAGEMENT_NAMESPACE
        return AuthInfo(
            isManagementNode = isManagement,
            namespace = namespace,
            nodeId = nodeId
        )
    }
}