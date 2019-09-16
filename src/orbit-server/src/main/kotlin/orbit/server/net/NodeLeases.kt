/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.launch
import orbit.server.concurrent.RuntimeScopes
import orbit.server.routing.LocalNodeInfo
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

typealias ChallengeToken = String

internal class NodeLeases(
    private val expiration: LeaseExpiration,
    private val nodeDirectory: NodeDirectory,
    private val localNodeInfo: LocalNodeInfo,
    private val runtimeScopes: RuntimeScopes
) : NodeManagementGrpc.NodeManagementImplBase() {

    override fun joinCluster(
        request: NodeManagementOuterClass.JoinClusterRequest,
        responseObserver: StreamObserver<NodeManagementOuterClass.NodeLease>
    ) {
        runtimeScopes.ioScope.launch {
            val nodeInfo = nodeDirectory.join(NodeInfo.ClientNodeInfo(visibleNodes = listOf(localNodeInfo.nodeInfo.id)))
            responseObserver.onNext(nodeInfo.lease.toProto())
            responseObserver.onCompleted()
        }
    }

    override fun renewLease(
        request: NodeManagementOuterClass.RenewLeaseRequest,
        responseObserver: StreamObserver<NodeManagementOuterClass.RenewLeaseResponse>
    ) {
        val nodeId = NodeId(request.nodeIdentity)

        val nodeInfo = nodeDirectory.getNode(nodeId)

        if (nodeInfo == null || !checkLease(nodeInfo, request.challengeToken)
        ) {
            responseObserver.onError(StatusException(Status.UNAUTHENTICATED))
            return
        }

        val lease = NodeLease(
            nodeId,
            challengeToken = request.challengeToken,
            expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.duration),
            renewAt = ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.renew)
        )

        runtimeScopes.ioScope.launch {
            nodeDirectory.report(
                when (nodeInfo) {
                    is NodeInfo.ServerNodeInfo -> nodeInfo.copy(
                        lease = lease,
                        visibleNodes = listOf(localNodeInfo.nodeInfo.id)
                    )
                    is NodeInfo.ClientNodeInfo -> nodeInfo.copy(
                        lease = lease,
                        visibleNodes = listOf(localNodeInfo.nodeInfo.id)
                    )
                }
            )
        }

        responseObserver.onNext(
            NodeManagementOuterClass.RenewLeaseResponse.newBuilder()
                .setLeaseRenewed(true)
                .setLeaseInfo(lease.toProto())
                .build()
        )
        responseObserver.onCompleted()
    }

    fun checkLease(nodeId: NodeId): Boolean {
        val lease = nodeDirectory.getNode(nodeId)
        return lease != null && checkLease(lease)
    }

    private fun checkLease(nodeInfo: NodeInfo, challengeToken: ChallengeToken? = null): Boolean {
        return nodeInfo.id.value.startsWith("mesh:") ||
                nodeInfo.lease.expiresAt > ZonedDateTime.now(ZoneOffset.UTC) &&
                (challengeToken == null || nodeInfo.lease.challengeToken == challengeToken)
    }

//    fun cullLeases(onExpire: (NodeLease) -> Unit) {
//        val now = ZonedDateTime.now(ZoneOffset.UTC)
//        val leaseCount = leases.count()
//
//        val (expiredLeases, validLeases) = leases.asIterable().partition { (id, lease) -> lease.expiresAt < now }
//
//        expiredLeases.forEach { (id) -> this.leases.remove(id) }
//        if (leases.count() != leaseCount) {
//            // TODO (brett) - remove this diagnostic message
//            println("Leases culled from $leaseCount to ${leases.count()}")
//        }
//    }

    data class LeaseExpiration(val duration: Duration, val renew: Duration)
}
