/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import orbit.server.auth.AuthSystem
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.server.pipeline.Pipeline
import orbit.shared.exception.AuthFailed
import orbit.shared.exception.InvalidNodeId
import orbit.shared.exception.toErrorContent
import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.net.Message
import orbit.shared.proto.Messages
import orbit.shared.proto.toMessageProto
import orbit.util.di.jvm.ComponentContainer
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager(
    private val runtimeScopes: RuntimeScopes,
    private val clusterManager: ClusterManager,
    private val localNodeInfo: LocalNodeInfo,
    private val authSystem: AuthSystem,
    container: ComponentContainer
) {
    private val connectedClients = ConcurrentHashMap<NodeId, ClientConnection>()

    // The pipeline needs to be lazy to avoid a stack overflow
    private val pipeline by container.inject<Pipeline>()

    fun getClient(nodeId: NodeId) = connectedClients[nodeId]

    fun onNewClient(
        namespace: Namespace,
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

                val authInfo = authSystem.attemptAuth(namespace, nodeId)
                authInfo ?: throw AuthFailed("Auth failled for $namespace $nodeId")

                // Create the connection
                val clientConnection = ClientConnection(authInfo, incomingChannel, outgoingChannel, pipeline)
                connectedClients[nodeId] = clientConnection

                // Update the visible nodes
                addNodesToDirectory(nodeInfo)

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

    private suspend fun addNodesToDirectory(nodeInfo: NodeInfo) {
        // Update the client's entry with this server
        clusterManager.updateNode(nodeInfo.id) {
            checkNotNull(it) { "The node '${nodeInfo.id}' could not be found in directory. " }
            it.copy(
                visibleNodes = it.visibleNodes + localNodeInfo.info.id
            )
        }

        // Update this server with client
        localNodeInfo.updateInfo {
            it.copy(
                visibleNodes = it.visibleNodes + nodeInfo.id
            )
        }
    }

    private suspend fun removeNodesFromDirectory(nodeInfo: NodeInfo) {
        // Update the client's entry
        clusterManager.updateNode(nodeInfo.id) {
            it?.copy(
                visibleNodes = it.visibleNodes - localNodeInfo.info.id
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