/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.server.pipeline.Pipeline
import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages
import org.kodein.di.Kodein
import org.kodein.di.erased.instance

internal class ClientConnections(val nodeId: NodeId, val nodeDirectory: NodeDirectory, val kodein: Kodein) :
    ConnectionGrpc.ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()

    fun getNode(nodeId: NodeId): MeshNode? {
        return clients[nodeId]
    }

    override fun messages(responseObserver: StreamObserver<Messages.Message>): StreamObserver<Messages.Message> {
        val remoteNodeId = NodeId(NodeIdServerInterceptor.NODE_ID.get())

        val connection = clients[remoteNodeId] ?: GrpcClient(remoteNodeId, responseObserver, kodein)
        clients[connection.id] = connection

        nodeDirectory.connectNode(NodeDirectory.NodeInfo(connection.id, parent = remoteNodeId))
        return connection
    }
}

