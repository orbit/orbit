/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.leasing

import kotlinx.coroutines.guava.await
import orbit.client.net.GrpcClient
import orbit.client.net.NodeStatus
import orbit.common.logging.logger
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import java.time.Instant

class NodeLeaser(private val nodeStatus: NodeStatus, grpcClient: GrpcClient) {
    private val logger by logger()
    private val nodeManagementStub = NodeManagementGrpc.newFutureStub(grpcClient.channel)

    suspend fun joinCluster() {
        logger.info("Joining Orbit cluster...")
        nodeManagementStub.joinCluster(
            NodeManagementOuterClass.JoinClusterRequest.newBuilder()
                .setCapabilities(
                    NodeManagementOuterClass.NodeCapabilities.newBuilder()
                        .addAllAddressableTypes(nodeStatus.capabilities).build()
                ).build()
        ).await().toNodeLease().also {
            nodeStatus.currentLease.set(it)
            logger.info("Joined cluster as node: ${it.nodeId}")
        }
    }

    suspend fun renewLease(force: Boolean) {
        nodeStatus.currentLease.get()?.let { existingLease ->
            if (force || existingLease.renewAt <= Instant.now()) {
                logger.debug("Renewing lease...")
                val renewalResult = nodeManagementStub.renewLease(
                    NodeManagementOuterClass.RenewLeaseRequest.newBuilder()
                        .setNodeIdentity(existingLease.nodeId)
                        .setChallengeToken(existingLease.challenge)
                        .setCapabilities(
                            NodeManagementOuterClass.NodeCapabilities.newBuilder()
                                .addAllAddressableTypes(nodeStatus.capabilities)
                                .build()
                        )
                        .build()
                ).await()
                if (renewalResult.leaseRenewed) {
                    nodeStatus.currentLease.set(renewalResult.leaseInfo.toNodeLease())
                    logger.debug("Lease renewed.")
                } else {
                    "Node renewal failed".also {
                        logger.error(it)
                        throw IllegalStateException(it)
                    }

                }
            }
        }
    }

    suspend fun tick() {
        renewLease(false)
    }
}