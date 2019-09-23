/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.leasing

import kotlinx.coroutines.guava.await
import orbit.client.net.GrpcClient
import orbit.common.logging.logger
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass

class LeaseManager(grpcClient: GrpcClient) {
    private val logger by logger()
    private val nodeManagementStub = NodeManagementGrpc.newFutureStub(grpcClient.channel)

    private var localLease: NodeLease? = null

    suspend fun joinCluster() {
        logger.info("Joining Orbit cluster...")
        nodeManagementStub.joinCluster(
            NodeManagementOuterClass.JoinClusterRequest.newBuilder()
                .setCapabilities(
                    NodeManagementOuterClass.NodeCapabilities.newBuilder()
                        .addAllAddressableTypes(
                            listOf("test")
                        ).build()
                ).build()
        ).await().asNodeLease().also {
            localLease = it
            logger.info("Joined cluster as node: ${it.nodeId}")
        }
    }

    suspend fun tick() {

    }
}