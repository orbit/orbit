/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Status
import io.grpc.StatusException
import orbit.server.routing.LocalNodeInfo
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import orbit.shared.proto.NodeManagementImplBase
import orbit.shared.proto.NodeManagementOuterClass
import java.time.ZoneOffset
import java.time.ZonedDateTime

typealias ChallengeToken = String

internal class NodeLeases(
    private val expiration: LeaseExpiration,
    private val nodeDirectory: NodeDirectory,
    private val localNodeInfo: LocalNodeInfo
) : NodeManagementImplBase() {

    override suspend fun joinCluster(request: NodeManagementOuterClass.JoinClusterRequest): NodeManagementOuterClass.NodeLease {
        val nodeInfo = nodeDirectory.join(NodeInfo.ClientNodeInfo(visibleNodes = listOf(localNodeInfo.nodeInfo.id)))
        return nodeInfo.lease.toProto()
    }

    override suspend fun renewLease(request: NodeManagementOuterClass.RenewLeaseRequest): NodeManagementOuterClass.RenewLeaseResponse {
        val nodeId = NodeId(request.nodeIdentity)

        val nodeInfo = nodeDirectory.getNode(nodeId)

        if (nodeInfo == null || !checkLease(nodeInfo, request.challengeToken)) {
            throw StatusException(Status.UNAUTHENTICATED)
        }

        val lease = NodeLease(
            nodeId,
            challengeToken = request.challengeToken,
            expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.duration),
            renewAt = ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.renew)
        )

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

        return NodeManagementOuterClass.RenewLeaseResponse.newBuilder()
            .setLeaseRenewed(true)
            .setLeaseInfo(lease.toProto())
            .build()
    }

    suspend fun checkLease(nodeId: NodeId): Boolean {
        val lease = nodeDirectory.getNode(nodeId)
        return lease != null && checkLease(lease)
    }

    private fun checkLease(nodeInfo: NodeInfo, challengeToken: ChallengeToken? = null): Boolean {
        return nodeInfo.id.value.startsWith("mesh:") ||
                nodeInfo.lease.expiresAt > ZonedDateTime.now(ZoneOffset.UTC) &&
                (challengeToken == null || nodeInfo.lease.challengeToken == challengeToken)
    }
}
