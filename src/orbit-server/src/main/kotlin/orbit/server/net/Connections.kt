/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.stub.StreamObserver
import orbit.common.di.ComponentProvider
import orbit.server.routing.LocalNodeInfo
import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages

internal class Connections(
    private val localNode: LocalNodeInfo,
    private val nodeDirectory: NodeDirectory,
    private val leases: NodeLeases,
    private val container: ComponentProvider
) : ConnectionGrpc.ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()
    private val meshNodes = HashMap<NodeId, GrpcMeshNodeClient>()

    fun getNode(nodeId: NodeId): MeshNode? {
        return clients[nodeId] ?: meshNodes[nodeId]
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

        clients[nodeId]?.onError(StatusException(Status.ALREADY_EXISTS))

        val connection = GrpcClient(nodeId, responseObserver, container) {
            nodeDirectory.removeNode(nodeId)
            clients.remove(nodeId)
            println("GRPC Client has closed: $nodeId")
        }
        clients[connection.id] = connection

        return connection
    }

    suspend fun refreshConnections() {
        val meshNodes = nodeDirectory.lookupMeshNodes().toList()

        meshNodes.filter { node -> !this.meshNodes.containsKey(node.id) && node.id != localNode.nodeInfo.id }
            .forEach { node ->
                val client = GrpcMeshNodeClient(node.id, node.host, node.port)

                this.meshNodes[node.id] = client
            }


        nodeDirectory.report(localNode.nodeInfo.copy(visibleNodes = this.meshNodes.keys.plus(clients.keys)))
    }
}
