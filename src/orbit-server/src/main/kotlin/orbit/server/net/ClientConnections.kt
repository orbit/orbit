/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages

internal class ClientConnections(val nodeId: NodeId, val nodeDirectory: NodeDirectory, val newClient: (StreamObserver<Messages.Message>) -> GrpcClient) :
    ConnectionGrpc.ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()

    fun getNode(nodeId: NodeId): MeshNode? {
        return clients[nodeId]
    }

    override fun messages(responseObserver: StreamObserver<Messages.Message>): StreamObserver<Messages.Message> {
        val nodeId = NodeId(NodeIdServerInterceptor.NODE_ID.get())

        val connection = clients[nodeId] ?: newClient(responseObserver)
        clients[connection.id] = connection

        nodeDirectory.connectNode(NodeDirectory.NodeInfo(connection.id, parent = nodeId))
        return connection
    }
}
