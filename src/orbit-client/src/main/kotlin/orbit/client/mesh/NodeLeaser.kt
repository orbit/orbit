/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.mesh

import kotlinx.coroutines.guava.await
import mu.KotlinLogging
import orbit.client.net.GrpcClient
import orbit.client.net.LocalNode
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.toCapabilitiesProto
import orbit.shared.proto.toNodeInfo
import orbit.util.time.Timestamp

internal class NodeLeaser(private val localNode: LocalNode, grpcClient: GrpcClient) {
    private val logger = KotlinLogging.logger { }

    private val nodeManagementStub = NodeManagementGrpc.newFutureStub(grpcClient.channel)

    suspend fun joinCluster() {
        logger.info("Joining cluster at '${localNode.status.serviceLocator}'...")

        nodeManagementStub.joinCluster(
            NodeManagementOuterClass.JoinClusterRequestProto.newBuilder()
                .setCapabilities(localNode.status.capabilities?.toCapabilitiesProto())
                .build()
        ).await().also { responseProto ->
            responseProto.info.toNodeInfo().also { nodeInfo ->
                localNode.manipulate {
                    it.copy(nodeInfo = nodeInfo)
                }
                logger.info("Joined cluster as node '${nodeInfo.id}'.")
            }

        }
    }

    suspend fun renewLease(force: Boolean) {
        localNode.status.nodeInfo?.let { existingInfo ->
            val existingLease = existingInfo.lease
            if (force || existingLease.renewAt.inPast()) {
                logger.debug("Renewing lease...")
                val renewalResult = nodeManagementStub.renewLease(
                    NodeManagementOuterClass.RenewNodeLeaseRequestProto.newBuilder()
                        .setChallengeToken(existingLease.challengeToken)
                        .setCapabilities(localNode.status.capabilities?.toCapabilitiesProto())
                        .build()
                ).await()

                check(renewalResult.status == NodeManagementOuterClass.NodeLeaseResponseProto.Status.OK) { "Node renewal failed" }
                localNode.manipulate {
                    it.copy(nodeInfo = renewalResult.info.toNodeInfo())
                }
                logger.debug("Lease renewed.")
            }
        }
    }

    suspend fun tick() {
        renewLease(false)
    }
}