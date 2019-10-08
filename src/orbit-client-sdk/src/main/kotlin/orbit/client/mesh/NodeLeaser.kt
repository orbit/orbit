/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.mesh

import kotlinx.coroutines.guava.await
import mu.KotlinLogging
import orbit.client.net.GrpcClient
import orbit.client.net.NodeStatus
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.toCapabilitiesProto
import orbit.shared.proto.toNodeInfo
import orbit.util.time.Timestamp
import orbit.util.time.now

class NodeLeaser(private val nodeStatus: NodeStatus, grpcClient: GrpcClient) {
    private val logger = KotlinLogging.logger { }

    private val nodeManagementStub = NodeManagementGrpc.newFutureStub(grpcClient.channel)

    suspend fun joinCluster() {
        logger.info("Joining cluster at '${nodeStatus.serviceLocator}'...")

        nodeManagementStub.joinCluster(
            NodeManagementOuterClass.JoinClusterRequestProto.newBuilder()
                .setCapabilities(nodeStatus.capabilities.toCapabilitiesProto())
                .build()
        ).await().also { responseProto ->
            responseProto.info.toNodeInfo().also { nodeInfo ->
                nodeStatus.latestInfo.set(nodeInfo)
                logger.info("Joined cluster as node '${nodeInfo.id}'.")
            }

        }
    }

    suspend fun renewLease(force: Boolean) {
        nodeStatus.latestInfo.get()?.let { existingInfo ->
            val existingLease = existingInfo.lease
            if (force || existingLease.renewAt <= Timestamp.now()) {
                logger.debug("Renewing lease...")
                val renewalResult = nodeManagementStub.renewLease(
                    NodeManagementOuterClass.RenewLeaseRequestProto.newBuilder()
                        .setChallengeToken(existingLease.challengeToken)
                        .setCapabilities(nodeStatus.capabilities.toCapabilitiesProto())
                        .build()
                ).await()

                check(renewalResult.status == NodeManagementOuterClass.RequestLeaseResponseProto.Status.OK) { "Node renewal failed" }
                nodeStatus.latestInfo.set(renewalResult.info.toNodeInfo())
                logger.debug("Lease renewed.")
            }
        }
    }

    suspend fun tick() {
        renewLease(false)
    }
}