/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import mu.KotlinLogging
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.ClusterManager
import orbit.shared.mesh.NodeStatus
import orbit.shared.proto.NodeManagementImplBase
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.getOrNull
import orbit.shared.proto.toCapabilities
import orbit.shared.proto.toNodeInfoProto
import orbit.shared.proto.toNodeLeaseRequestResponseProto

class NodeManagementService(
    private val clusterManager: ClusterManager,
    runtimeScopes: RuntimeScopes
) : NodeManagementImplBase(runtimeScopes.ioScope.coroutineContext) {
    private val logger = KotlinLogging.logger { }
    override suspend fun joinCluster(request: NodeManagementOuterClass.JoinClusterRequestProto): NodeManagementOuterClass.NodeLeaseResponseProto =
        try {
            val namespace = ServerAuthInterceptor.NAMESPACE.get()
            val capabilities = request.capabilities.toCapabilities()
            val info = clusterManager.joinCluster(
                namespace = namespace,
                capabilities = capabilities,
                nodeStatus = NodeStatus.ACTIVE
            )
            logger.debug("Joining cluster ${info.id}")
            info.toNodeLeaseRequestResponseProto()
        } catch (t: Throwable) {
            t.toNodeLeaseRequestResponseProto()
        }


    override suspend fun renewLease(request: NodeManagementOuterClass.RenewNodeLeaseRequestProto): NodeManagementOuterClass.NodeLeaseResponseProto =
        try {
            val nodeId = ServerAuthInterceptor.NODE_ID.getOrNull()
            checkNotNull(nodeId) { "Node ID was not specified" }
            val capabilities = request.capabilities.toCapabilities()
            val challengeToken = request.challengeToken
            val info = clusterManager.renewLease(
                nodeId = nodeId,
                challengeToken = challengeToken,
                capabilities = capabilities
            )
            info.toNodeLeaseRequestResponseProto()
        } catch (t: Throwable) {
            t.toNodeLeaseRequestResponseProto()
        }

    override suspend fun leaveCluster(request: NodeManagementOuterClass.LeaveClusterRequestProto): NodeManagementOuterClass.NodeLeaseResponseProto {

        val nodeId = ServerAuthInterceptor.NODE_ID.getOrNull()
        checkNotNull(nodeId) { "Node ID was not specified" }

        val nodeInfo = clusterManager.updateNode(nodeId) {
            logger.debug("The node '${nodeId}' was not found in directory while leaving the cluster.")
            it?.copy(
                nodeStatus = NodeStatus.DRAINING
            )
        }

        return NodeManagementOuterClass.NodeLeaseResponseProto.newBuilder()
            .setInfo(nodeInfo.toNodeInfoProto())
            .build()
    }
}