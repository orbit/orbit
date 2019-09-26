/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Status
import io.grpc.StatusException
import orbit.server.proto.toProto
import orbit.server.routing.LocalNodeInfo
import orbit.server.routing.NodeCapabilities
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import orbit.shared.proto.NodeManagementImplBase
import orbit.shared.proto.NodeManagementOuterClass
import java.lang.IllegalArgumentException
import java.time.Instant

typealias ChallengeToken = String

internal class NodeLeases(
    private val expiration: LeaseExpiration,
    private val nodeDirectory: NodeDirectory,
    private val localNodeInfo: LocalNodeInfo
) : NodeManagementImplBase() {

    override suspend fun joinCluster(request: NodeManagementOuterClass.JoinClusterRequest): NodeManagementOuterClass.NodeLease {
        val nodeInfo = nodeDirectory.join(
            NodeInfo.ClientNodeInfo(
                capabilities = NodeCapabilities(
                    addressableTypes = request.capabilities.addressableTypesList
                )
            )
        )
        nodeDirectory.report(nodeInfo)

        return nodeInfo.lease.toProto()
    }

    override suspend fun renewLease(request: NodeManagementOuterClass.RenewLeaseRequest): NodeManagementOuterClass.RenewLeaseResponse {
        val nodeId = NodeId(NodeIdServerInterceptor.NODE_ID.get())
        val nodeInfo = nodeDirectory.getNode(nodeId)

        if (nodeInfo == null || !checkLease(nodeInfo, request.challengeToken)) {
            throw StatusException(Status.UNAUTHENTICATED)
        }

        val renewedNode = renewLease(nodeInfo)
        nodeDirectory.report(renewedNode)

        return NodeManagementOuterClass.RenewLeaseResponse.newBuilder()
            .setLeaseRenewed(true)
            .setLeaseInfo(renewedNode.lease.toProto())
            .build()
    }

    suspend fun <TNodeInfo: NodeInfo> renewLease(nodeInfo: TNodeInfo): TNodeInfo {

        val lease = nodeInfo.lease.copy(
            expiresAt = Instant.now().plus(expiration.duration),
            renewAt = Instant.now().plus(expiration.renew)
        )

        val renewedNode = when (nodeInfo) {
            is NodeInfo.ServerNodeInfo -> nodeInfo.copy(
                lease = lease
            )
            is NodeInfo.ClientNodeInfo -> nodeInfo.copy(
                lease = lease
            )
            else -> throw IllegalArgumentException()
        }

        nodeDirectory.report(renewedNode)

        return renewedNode as TNodeInfo
    }

    suspend fun checkLease(nodeId: NodeId): Boolean {
        val lease = nodeDirectory.getNode(nodeId)
        return lease != null && checkLease(lease)
    }

    private fun checkLease(nodeInfo: NodeInfo, challengeToken: ChallengeToken? = null): Boolean {
        return nodeInfo.id.value.startsWith("mesh:") ||
                nodeInfo.lease.expiresAt > Instant.now() &&
                (challengeToken == null || nodeInfo.lease.challengeToken == challengeToken)
    }
}
