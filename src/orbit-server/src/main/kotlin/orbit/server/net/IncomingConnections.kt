/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.launch
import orbit.common.di.ComponentProvider
import orbit.server.concurrent.RuntimeScopes
import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages

internal class IncomingConnections(
    private val localNode: NodeInfo.LocalServerNodeInfo,
    private val nodeDirectory: NodeDirectory,
    private val leases: NodeLeases,
    private val container: ComponentProvider
) : ConnectionGrpc.ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()

    fun getNode(nodeId: NodeId): MeshNode? {
        return clients[nodeId]
    }

    override fun messages(responseObserver: StreamObserver<Messages.Message>): StreamObserver<Messages.Message>? {
        val nodeId = NodeId(NodeIdServerInterceptor.NODE_ID.get())

        if (!leases.checkLease(nodeId)) {
            responseObserver.onNext(
                Messages.Message.newBuilder().setInvocationError(
                    Messages.InvocationErrorResponse.newBuilder()
                        .setMessage(StatusException(Status.UNAUTHENTICATED).toString())
                        .setStatusValue(Messages.InvocationErrorResponse.Status.UNAUTHENTICATED_VALUE)
                ).build()
            )

            responseObserver.onError(StatusException(Status.UNAUTHENTICATED))
            return null
        }
        val runtimeScopes by container.inject<RuntimeScopes>()

        clients[nodeId]?.onError(StatusException(Status.ALREADY_EXISTS))

        val connection = GrpcClient(nodeId, responseObserver, container) {
            nodeDirectory.removeNode(nodeId)
            clients.remove(nodeId)
            println("GRPC Client has closed: $nodeId")
        }
        clients[connection.id] = connection

        return connection
    }
}
