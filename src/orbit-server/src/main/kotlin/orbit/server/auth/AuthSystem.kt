/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.auth

import orbit.server.mesh.MANAGEMENT_NAMESPACE
import orbit.server.service.ServerAuthInterceptor.Keys.NODE_ID
import orbit.shared.mesh.NodeId
import orbit.shared.proto.getOrNull

data class AuthInfo(
    val isManagementNode: Boolean,
    val nodeId: NodeId
)

class AuthSystem {
    suspend fun auth() = auth(NODE_ID.getOrNull())

    suspend fun auth(nodeId: NodeId?): AuthInfo? {
        nodeId ?: return null

        val isManagement = nodeId.namespace == MANAGEMENT_NAMESPACE
        return AuthInfo(
            isManagementNode = isManagement,
            nodeId = nodeId
        )
    }


}