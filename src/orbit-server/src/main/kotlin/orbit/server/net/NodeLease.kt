/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.util.toProto
import java.time.Instant

class NodeLease(
    val nodeId: NodeId,
    val challengeToken: ChallengeToken,
    val expiresAt: Instant,
    val renewAt: Instant
) {
    companion object Statics {
        @JvmStatic
        val Empty = NodeLease(
            NodeId.Empty,
            "",
            Instant.MIN,
            Instant.MIN
        )
    }

    fun toProto(): NodeManagementOuterClass.NodeLease {
        return NodeManagementOuterClass.NodeLease.newBuilder()
            .setNodeIdentity(nodeId.value)
            .setChallengeToken(challengeToken)
            .setRenewAt(renewAt.toProto())
            .setExpiresAt(expiresAt.toProto())
            .build()
    }
}
