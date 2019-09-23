/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.leasing

import orbit.shared.proto.NodeManagementOuterClass
import java.time.Instant

data class NodeLease(
    val nodeId: String,
    val challenge: String,
    val expiresAt: Instant,
    val renewAt: Instant
)

fun  NodeManagementOuterClass.NodeLease.asNodeLease() = NodeLease(
    nodeId = this.nodeIdentity,
    challenge = this.challengeToken,
    expiresAt = Instant.ofEpochSecond(this.expiresAt.seconds),
    renewAt = Instant.ofEpochSecond(this.renewAt.seconds)
)