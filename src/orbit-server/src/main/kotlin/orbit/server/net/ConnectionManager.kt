/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import orbit.server.auth.AuthSystem
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.MANAGEMENT_NAMESPACE
import orbit.server.pipeline.Pipeline
import orbit.server.service.Meters
import orbit.shared.exception.AuthFailed
import orbit.shared.exception.InvalidNodeId
import orbit.shared.exception.toErrorContent
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.net.Message
import orbit.shared.proto.Messages
import orbit.shared.proto.toMessageProto
import orbit.util.di.ComponentContainer
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager(
    private val runtimeScopes: RuntimeScopes,
    private val clusterManager: ClusterManager,
    private val localNodeInfo: LocalNodeInfo,
    private val authSystem: AuthSystem,
    container: ComponentContainer
) {
    private val connectedClients = ConcurrentHashMap<NodeId, ClientConnection>()

    init {
        Metrics.gauge(Meters.Names.ConnectedClients, connectedClients) { c -> c.count().toDouble() }
    }

    // The pipeline needs to be lazy to avoid a stack overflow
    private val pipeline by container.inject<Pipeline>()

    fun getClient(nodeId: NodeId) = connectedClients[nodeId]

    val clients get() = connectedClients.values.map { c -> c.nodeId }.toList()

    fun onNewClient(
        nodeId: NodeId,
        incomingChannel: ReceiveChannel<Messages.MessageProto>,
        outgoingChannel: SendChannel<Messages.MessageProto>
    ) {
        runtimeScopes.ioScope.launch {
            var nodeInfo: NodeInfo? = null
            try {
                // Verify the node is valid
                nodeInfo = clusterManager.getNode(nodeId)
                if (nodeInfo == null) throw InvalidNodeId(nodeId)

                val authInfo = authSystem.auth(nodeId)
                authInfo ?: throw AuthFailed("Auth failed for $nodeId")

                // Create the connection
                val clientConnection = ClientConnection(authInfo, incomingChannel, outgoingChannel, pipeline)
                connectedClients[nodeId] = clientConnection

                // Update the client's entry with this server
                clusterManager.updateNode(nodeInfo.id) {
                    checkNotNull(it) { "The node '${nodeInfo.id}' could not be found in directory." }
                    val visibleNodes = it.visibleNodes + localNodeInfo.info.id
                    it.copy(
                        visibleNodes = visibleNodes
                    )
                }

                // Update the visible nodes
                updateDirectoryClients()

                // Consume messages, suspends here until connection drops
                clientConnection.consumeMessages()
            } catch (t: Throwable) {
                outgoingChannel.send(
                    Message(
                        content = t.toErrorContent()
                    ).toMessageProto()
                )
                outgoingChannel.close()
            } finally {
                // Remove from node directory if it was set
                nodeInfo?.also {
                    removeNodesFromDirectory(it)
                }

                // Remove client
                connectedClients.remove(nodeId)
            }
        }
    }

    private suspend fun updateDirectoryClients() {
        val visibleNodes = localNodeInfo.info.visibleNodes
            .filter { node -> node.namespace == MANAGEMENT_NAMESPACE }
            .plus(connectedClients.values.map { n -> n.nodeId }).toSet()

        // Update this server with client
        localNodeInfo.updateInfo {
            it.copy(
                visibleNodes = visibleNodes
            )
        }
    }

    private suspend fun removeNodesFromDirectory(nodeInfo: NodeInfo) {
        // Update the client's entry
        clusterManager.updateNode(nodeInfo.id) {
            val visibleNodes = it!!.visibleNodes - localNodeInfo.info.id

            it?.copy(
                visibleNodes = visibleNodes
            )
        }

        // Update this server
        localNodeInfo.updateInfo {
            it.copy(
                visibleNodes = it.visibleNodes - nodeInfo.id
            )
        }
    }
}