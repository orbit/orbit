/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.proto.Messages
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager(
    private val runtimeScopes: RuntimeScopes,
    private val clusterManager: ClusterManager,
    private val localNodeInfo: LocalNodeInfo
) {
    private val connectedClients = ConcurrentHashMap<NodeId, ClientConnection>()

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
                checkNotNull(nodeInfo)

                // Create the connection
                val clientConnection = ClientConnection(nodeId, incomingChannel, outgoingChannel)
                connectedClients[nodeId] = clientConnection

                // Update the visible nodes
                addNodesToDirectory(nodeInfo)

                // Consume messages, suspends here until connection drops
                clientConnection.consumeMessages()
            } catch (t: Throwable) {
                outgoingChannel.close(t)
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
            checkNotNull(it) { "The node '${nodeInfo.id}' could not be found in directory. "}
            it.copy(
                visibleNodes = it.visibleNodes + localNodeInfo.info.id
            )
        }

        // Update this server with client
        localNodeInfo.updateInfo {
            it.copy(
                visibleNodes = it.visibleNodes + localNodeInfo.info.id
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
                visibleNodes = it.visibleNodes - localNodeInfo.info.id
            )
        }
    }
}