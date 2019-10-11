/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.ClusterManager
import orbit.shared.proto.NodeManagementImplBase
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.getOrNull
import orbit.shared.proto.toCapabilities
import orbit.shared.proto.toLeaseRequestResponseProto

class NodeManagementService(
    private val clusterManager: ClusterManager,
    runtimeScopes: RuntimeScopes
) : NodeManagementImplBase(runtimeScopes.ioScope.coroutineContext) {
    override suspend fun joinCluster(request: NodeManagementOuterClass.JoinClusterRequestProto): NodeManagementOuterClass.NodeLeaseResponseProto =
        try {
            val namespace = ServerAuthInterceptor.NAMESPACE.get()
            val capabilities = request.capabilities.toCapabilities()
            val info = clusterManager.joinCluster(
                namespace = namespace,
                capabilities = capabilities
            )
            info.toLeaseRequestResponseProto()
        } catch (t: Throwable) {
            t.toLeaseRequestResponseProto()
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
            info.toLeaseRequestResponseProto()
        } catch (t: Throwable) {
            t.toLeaseRequestResponseProto()
        }

}