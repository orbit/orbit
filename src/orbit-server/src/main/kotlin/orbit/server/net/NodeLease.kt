/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import com.google.protobuf.Timestamp
import orbit.shared.proto.NodeManagementOuterClass
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAccessor

class NodeLease(
    val nodeId: NodeId,
    val challengeToken: ChallengeToken,
    val expiresAt: ZonedDateTime,
    val renewAt: ZonedDateTime
) {
    companion object Statics {
        @JvmStatic
        val Empty = NodeLease(
            NodeId.Empty,
            "",
            ZonedDateTime.now(),
            ZonedDateTime.of(LocalDate.EPOCH, LocalTime.MIDNIGHT, ZoneId.systemDefault())
        )
    }

    fun toProto(): NodeManagementOuterClass.NodeLease {
        return NodeManagementOuterClass.NodeLease.newBuilder()
            .setNodeIdentity(nodeId.value)
            .setChallengeToken(challengeToken)
            .setRenewAt(Timestamp.newBuilder().setSeconds(renewAt.toEpochSecond()))
            .setExpiresAt(Timestamp.newBuilder().setSeconds(expiresAt.toEpochSecond()))
            .build()
    }
}
