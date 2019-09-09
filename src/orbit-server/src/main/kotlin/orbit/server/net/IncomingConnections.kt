/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.launch
import orbit.common.di.ComponentProvider
import orbit.server.concurrent.RuntimeScopes
import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages

internal class IncomingConnections(
    private val localNode: LocalNodeId,
    private val nodeDirectory: NodeDirectory,
    private val container: ComponentProvider
) :
    ConnectionGrpc.ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()

    fun getNode(nodeId: NodeId): MeshNode? {
        return clients[nodeId]
    }

    override fun messages(responseObserver: StreamObserver<Messages.Message>): StreamObserver<Messages.Message> {
        val nodeId = NodeId(NodeIdServerInterceptor.NODE_ID.get())
        val runtimeScopes by container.inject<RuntimeScopes>()

        val connection = clients[nodeId] ?: GrpcClient(nodeId, responseObserver, container) {
            nodeDirectory.removeNode(nodeId)
            println("GRPC Client has closed: $nodeId")
        }
        clients[connection.id] = connection

        runtimeScopes.ioScope.launch {
            nodeDirectory.connectNode(NodeDirectory.NodeInfo.ClientNodeInfo(connection.id, listOf(localNode.nodeId)))
        }
        return connection
    }
}

