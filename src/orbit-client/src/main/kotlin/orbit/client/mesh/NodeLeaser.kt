/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.mesh

import io.grpc.Deadline
import kotlinx.coroutines.guava.await
import mu.KotlinLogging
import orbit.client.OrbitClientConfig
import orbit.client.net.GrpcClient
import orbit.client.net.LocalNode
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.toCapabilitiesProto
import orbit.shared.proto.toNodeInfo
import java.util.concurrent.TimeUnit

internal class NodeLeaser(private val localNode: LocalNode, grpcClient: GrpcClient, config: OrbitClientConfig) {
    private val logger = KotlinLogging.logger { }
    private val joinTimeout = config.joinClusterTimeout

    private val nodeManagementStub = NodeManagementGrpc.newFutureStub(grpcClient.channel)

    suspend fun joinCluster() {
        logger.info("Joining namespace '${localNode.status.namespace}' in the '${localNode.status.grpcEndpoint}' cluster ...")
        nodeManagementStub
            .withWaitForReady()
            .withDeadline(Deadline.after(joinTimeout.toMillis(), TimeUnit.MILLISECONDS))
            .joinCluster(
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

                if (renewalResult.status != NodeManagementOuterClass.NodeLeaseResponseProto.Status.OK) {
                    throw NodeLeaseRenewalFailed("Node renewal failed")
                }

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