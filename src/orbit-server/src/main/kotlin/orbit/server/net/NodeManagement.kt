/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

typealias ChallengeToken = String

internal class NodeManagement(private val expiration: LeaseExpiration) : NodeManagementGrpc.NodeManagementImplBase() {

    override fun joinCluster(
        request: NodeManagementOuterClass.JoinClusterRequest,
        responseObserver: StreamObserver<NodeManagementOuterClass.NodeLease>
    ) {
        val lease = NodeLease(
            NodeId.generate(),
            "challenge",
            ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.duration),
            ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.renew)
        )

        responseObserver.onNext(lease.toProto())
        responseObserver.onCompleted()
    }

    override fun renewLease(
        request: NodeManagementOuterClass.RenewLeaseRequest,
        responseObserver: StreamObserver<NodeManagementOuterClass.RenewLeaseResponse>
    ) {
        val lease = NodeLease(
            NodeId(request.nodeIdentity),
            request.challengeToken,
            ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.duration),
            ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.renew)
        )

        responseObserver.onNext(
            NodeManagementOuterClass.RenewLeaseResponse.newBuilder()
                .setLeaseRenewed(true)
                .setLeaseInfo(lease.toProto())
                .build()
        )
        responseObserver.onCompleted()
    }

    data class LeaseExpiration(val duration: Duration, val renew: Duration)
}
