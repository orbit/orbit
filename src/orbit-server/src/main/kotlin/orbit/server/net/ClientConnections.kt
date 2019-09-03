/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.server.pipeline.Pipeline
import orbit.server.routing.NodeDirectory
import orbit.server.routing.Router
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages

internal class ClientConnections(val pipeline: Pipeline, val nodeId: NodeId, val nodeDirectory: NodeDirectory) :
    ConnectionGrpc.ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()

    override fun messages(responseObserver: StreamObserver<Messages.Message>): StreamObserver<Messages.Message> {
        val nodeId = NodeId(ConnectionInterceptor.NODE_ID.get())

        val connection = clients[nodeId] ?: GrpcClient(responseObserver = responseObserver) {
            pipeline.pushInbound(it)
        }
        clients[connection.id] = connection

        nodeDirectory.connectNode(connection, nodeId)
        return connection
    }
}
