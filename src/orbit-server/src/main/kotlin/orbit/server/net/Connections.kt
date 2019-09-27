/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import orbit.common.di.ComponentProvider
import orbit.server.routing.LocalNodeInfo
import orbit.server.routing.MeshNode
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import orbit.shared.proto.ConnectionImplBase
import orbit.shared.proto.Messages
import java.time.Instant

internal class Connections(
    private val localNode: LocalNodeInfo,
    private val nodeDirectory: NodeDirectory,
    private val leases: NodeLeases,
    private val container: ComponentProvider
) : ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()
    private val meshNodes = HashMap<NodeId, GrpcMeshNodeClient>()

    fun getNode(nodeId: NodeId): MeshNode? {
        return clients[nodeId] ?: meshNodes[nodeId]
    }

    override fun messages(requests: ReceiveChannel<Messages.Message>) = produce<Messages.Message> {
        val nodeId = NodeId(NodeIdServerInterceptor.NODE_ID.get())

        if (!leases.checkLease(nodeId)) {

            send(
                Messages.Message.newBuilder().setInvocationError(
                    Messages.InvocationErrorResponse.newBuilder()
                        .setMessage(StatusException(Status.UNAUTHENTICATED).toString())
                        .setStatusValue(Messages.InvocationErrorResponse.Status.UNAUTHENTICATED_VALUE)
                ).build()
            )

            throw StatusException(Status.UNAUTHENTICATED)
        }

        val connection = GrpcClient(nodeId, requests, { message -> send(message) }, container)
        clients[connection.id] = connection

        nodeDirectory.report(localNode.nodeInfo.copy(visibleNodes = localNode.nodeInfo.visibleNodes.plus(connection.id)))

        val clientNode = nodeDirectory.getNode(nodeId)
        if (clientNode != null) {
            nodeDirectory.report(clientNode.clone(visibleNodes = clientNode.visibleNodes.plus(localNode.nodeInfo.id).toSet()))
        }

        connection.ready()

        clients.remove(nodeId)
        println("GRPC Client has closed: $nodeId")
    }

    suspend fun refreshConnections() {
        val meshNodes = nodeDirectory.lookupMeshNodes().toList()
        var newConnections = false
        meshNodes.filter { node -> !this.meshNodes.containsKey(node.id) && node.id != localNode.nodeInfo.id }
            .filter { node -> node.host != localNode.nodeInfo.host }
            .forEach { node ->
                newConnections = true
                this.meshNodes[node.id] = GrpcMeshNodeClient(node.id, node.host, node.port)
            }

        if (localNode.nodeInfo.lease.renewAt < Instant.now()) {
            localNode.updateNodeInfo(leases.renewLease(localNode.nodeInfo))
        }

        if (newConnections) {
            println("Updating local node ${localNode.nodeInfo.id} in directory ${this.meshNodes.keys.plus(clients.keys)}")
            nodeDirectory.report(localNode.nodeInfo.copy(visibleNodes = this.meshNodes.keys.plus(clients.keys)))
        }
    }
}