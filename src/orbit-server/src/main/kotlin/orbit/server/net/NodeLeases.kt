/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.stub.StreamObserver
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

typealias ChallengeToken = String

internal class NodeLeases(private val expiration: LeaseExpiration) : NodeManagementGrpc.NodeManagementImplBase() {

    private val leases = ConcurrentHashMap<NodeId, NodeLease>()

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

        leases[lease.nodeId] = lease

        responseObserver.onNext(lease.toProto())
        responseObserver.onCompleted()
    }

    override fun renewLease(
        request: NodeManagementOuterClass.RenewLeaseRequest,
        responseObserver: StreamObserver<NodeManagementOuterClass.RenewLeaseResponse>
    ) {
        if (!checkLease(NodeId(request.nodeIdentity), request.challengeToken)
        ) {
            responseObserver.onError(StatusException(Status.UNAUTHENTICATED))
            return
        }

        val lease = NodeLease(
            NodeId(request.nodeIdentity),
            request.challengeToken,
            ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.duration),
            ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.renew)
        )

        leases[lease.nodeId] = lease

        responseObserver.onNext(
            NodeManagementOuterClass.RenewLeaseResponse.newBuilder()
                .setLeaseRenewed(true)
                .setLeaseInfo(lease.toProto())
                .build()
        )
        responseObserver.onCompleted()
    }

    fun checkLease(nodeId: NodeId, challengeToken: ChallengeToken? = null): Boolean {
        val lease = leases[nodeId]
        return lease != null &&
                lease.expiresAt > ZonedDateTime.now(ZoneOffset.UTC) &&
                (challengeToken == null || lease.challengeToken == challengeToken)
    }

    fun cullLeases() {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val leaseCount = leases.count()
        leases.forEach { (id, lease) -> if (lease.expiresAt < now) leases.remove(id) }
        if (leases.count() != leaseCount) {
            // TODO (brett) - remove this diagnostic message
            println("Leases culled from $leaseCount to ${leases.count()}")
        }
    }

    data class LeaseExpiration(val duration: Duration, val renew: Duration)
}
