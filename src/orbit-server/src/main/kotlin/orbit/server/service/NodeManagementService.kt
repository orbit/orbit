/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.ClusterManager
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.proto.NodeManagementImplBase
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.toLeaseRequestResponseProto

class NodeManagementService(
    private val clusterManager: ClusterManager,
    private val localNodeInfo: LocalNodeInfo,
    runtimeScopes: RuntimeScopes
) : NodeManagementImplBase(runtimeScopes.ioScope.coroutineContext) {
    override suspend fun joinCluster(request: NodeManagementOuterClass.JoinClusterRequestProto): NodeManagementOuterClass.RequestLeaseResponseProto =
        try {
            val namespace = ServerAuthInterceptor.NAMESPACE.get()
            val addressableTypes = request.addressableTypesList.toList()
            val info = clusterManager.joinCluster(
                namespace = namespace,
                capabilities = NodeCapabilities(
                    addressableTypes = addressableTypes
                )
            )
            info.toLeaseRequestResponseProto()
        } catch (t: Throwable) {
            t.toLeaseRequestResponseProto()
        }


    override suspend fun renewLease(request: NodeManagementOuterClass.RenewLeaseRequestProto): NodeManagementOuterClass.RequestLeaseResponseProto =
        try {
            val nodeId = ServerAuthInterceptor.NODE_ID.get()
            val addressableTypes = request.addressableTypesList.toList()
            val challengeToken = request.challengeToken
            val info = clusterManager.renewLease(
                nodeId = nodeId,
                challengeToken = challengeToken,
                capabilities = NodeCapabilities(
                    addressableTypes = addressableTypes
                )
            )
            info.toLeaseRequestResponseProto()
        } catch (t: Throwable) {
            t.toLeaseRequestResponseProto()
        }

}