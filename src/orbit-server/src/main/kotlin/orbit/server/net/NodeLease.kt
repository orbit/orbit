/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import com.google.protobuf.Timestamp
import orbit.shared.proto.NodeManagementOuterClass
import java.time.ZonedDateTime

class NodeLease(
    val nodeId: NodeId,
    val challengeToken: ChallengeToken,
    val renewAt: ZonedDateTime,
    val expiresAt: ZonedDateTime
) {
    fun toProto(): NodeManagementOuterClass.NodeLease {
        return NodeManagementOuterClass.NodeLease.newBuilder()
            .setNodeIdentity(nodeId.value)
            .setChallengeToken(challengeToken)
            .setRenewAt(Timestamp.newBuilder().setSeconds(renewAt.toEpochSecond()))
            .setExpiresAt(Timestamp.newBuilder().setSeconds(expiresAt.toEpochSecond()))
            .build()
    }
}
