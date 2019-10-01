/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.mesh.InvalidChallengeException
import orbit.shared.mesh.LeaseExpiredException
import orbit.shared.mesh.NodeInfo

fun NodeInfo.toLeaseRequestResponseProto(): NodeManagementOuterClass.RequestLeaseResponseProto =
    NodeManagementOuterClass.RequestLeaseResponseProto.newBuilder()
        .setStatus(NodeManagementOuterClass.RequestLeaseResponseProto.Status.OK)
        .setNodeIdentity(id.value)
        .setChallengeToken(lease.challengeToken)
        .setExpiresAt(lease.expiresAt.toTimestampProto())
        .setRenewAt(lease.renewAt.toTimestampProto())
        .build()

fun Throwable.toLeaseRequestResponseProto(): NodeManagementOuterClass.RequestLeaseResponseProto =
    NodeManagementOuterClass.RequestLeaseResponseProto.newBuilder()
        .setStatus(
            when (this) {
                is LeaseExpiredException -> NodeManagementOuterClass.RequestLeaseResponseProto.Status.INVALID_LEASE
                is InvalidChallengeException -> NodeManagementOuterClass.RequestLeaseResponseProto.Status.INVALID_TOKEN
                else -> NodeManagementOuterClass.RequestLeaseResponseProto.Status.UNKNOWN_ERROR
            }
        ).build()