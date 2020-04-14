/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.micrometer.core.instrument.Metrics
import mu.KotlinLogging
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.MANAGEMENT_NAMESPACE
import orbit.server.service.Meters
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeStatus
import java.util.concurrent.ConcurrentHashMap

class RemoteMeshNodeManager(
    private val localNode: LocalNodeInfo,
    private val clusterManager: ClusterManager
) {
    private val logger = KotlinLogging.logger { }
    private val connections = ConcurrentHashMap<NodeId, RemoteMeshNodeConnection>()

    init {
        Metrics.gauge(Meters.Names.ConnectedNodes, connections) { c -> c.count().toDouble() }
    }

    suspend fun tick() {
        refreshConnections()
    }

    fun getNode(nodeId: NodeId): RemoteMeshNodeConnection? {
        return connections[nodeId]
    }

    suspend fun refreshConnections() {
        val allNodes = clusterManager.getAllNodes()
        val addedNodes = ArrayList<NodeId>()
        val removedNodes = ArrayList<NodeId>()

        val meshNodes = allNodes
            .filter { node -> node.nodeStatus == NodeStatus.ACTIVE }
            .filter { node -> node.id.namespace == MANAGEMENT_NAMESPACE }
            .filter { node -> !this.connections.containsKey(node.id) }
            .filter { node -> node.id != localNode.info.id }
            .filter { node -> node.url != null && node.url != localNode.info.url }

        meshNodes.forEach { node ->
            logger.info("Connecting to peer ${node.id.key} @${node.url}...")
            this.connections[node.id] = RemoteMeshNodeConnection(localNode, node)
            addedNodes.add(node.id)
        }

        connections.values.forEach { node ->
            if (allNodes.none { it.id == node.id }) {
                logger.info("Removing peer ${node.id.key}...")
                connections[node.id]!!.disconnect()
                connections.remove(node.id)
                removedNodes.add(node.id)
            }
        }

        if (addedNodes.isNotEmpty() || removedNodes.isNotEmpty()) {
            clusterManager.updateNode(localNode.info.id) { node ->
                node!!.copy(visibleNodes = node.visibleNodes.plus(addedNodes).minus(removedNodes))
            }
        }
    }
}
