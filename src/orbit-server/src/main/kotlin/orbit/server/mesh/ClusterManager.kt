/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import mu.KotlinLogging
import orbit.server.OrbitServerConfig
import orbit.shared.exception.InvalidChallengeException
import orbit.shared.exception.InvalidNodeId
import orbit.shared.mesh.ChallengeToken
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeLease
import orbit.shared.mesh.NodeStatus
import orbit.util.misc.RNGUtils
import orbit.util.time.Clock
import orbit.util.time.toTimestamp
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ClusterManager(
    config: OrbitServerConfig,
    private val clock: Clock,
    private val nodeDirectory: NodeDirectory
) {
    private val logger = KotlinLogging.logger { }
    private val leaseExpiration = config.nodeLeaseDuration
    private val clusterNodes = ConcurrentHashMap<NodeId, NodeInfo>()
    private val nodeGraph = AtomicReference<Graph<NodeId, DefaultEdge>>()

    fun getAllNodes() = clusterNodes.filter { clock.inFuture(it.value.lease.expiresAt) }.values

    suspend fun tick() {
        val allNodes = nodeDirectory.entries()
        clusterNodes.clear()
        clusterNodes.putAll(allNodes)
        buildGraph()
    }

    suspend fun joinCluster(
        namespace: String,
        capabilities: NodeCapabilities,
        url: String? = null,
        nodeStatus: NodeStatus,
        nodeId: NodeId? = null
    ): NodeInfo {
        do {
            val newNodeId = nodeId ?: NodeId.generate(namespace)

            val lease = NodeLease(
                challengeToken = RNGUtils.randomString(64),
                expiresAt = clock.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                renewAt = clock.now().plus(leaseExpiration.renewIn).toTimestamp()
            )

            val info = NodeInfo(
                id = newNodeId,
                capabilities = capabilities,
                lease = lease,
                url = url,
                nodeStatus = nodeStatus
            )

            if (nodeDirectory.compareAndSet(newNodeId, null, info)) {
                tick()
                return info
            }
        } while (true)
    }

    suspend fun renewLease(nodeId: NodeId, challengeToken: ChallengeToken, capabilities: NodeCapabilities): NodeInfo =
        updateNode(nodeId) { initialValue ->
            if (initialValue == null || clock.inPast(initialValue.lease.expiresAt)) {
                throw InvalidNodeId(nodeId)
            }

            if (initialValue.lease.challengeToken != challengeToken) {
                throw InvalidChallengeException(nodeId, challengeToken)
            }

            val newValue = initialValue.copy(
                capabilities = capabilities,
                lease = initialValue.lease.copy(
                    expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                    renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
                ),
                visibleNodes = initialValue.visibleNodes.intersect(clusterNodes.keys().toList())
            )

            newValue
        }!!.also {
            tick()
        }

    suspend fun updateNode(nodeId: NodeId, body: (NodeInfo?) -> NodeInfo?): NodeInfo? =
        nodeDirectory.manipulate(nodeId, body)
            .also { tick() }

    suspend fun getNode(nodeId: NodeId, forceRefresh: Boolean = false): NodeInfo? =
        if (forceRefresh) {
            nodeDirectory.get(nodeId)
        } else {
            try {
                clusterNodes.getOrPut(nodeId) {
                    nodeDirectory.get(nodeId)
                }
            } catch (t: NullPointerException) {
                null
            }
        }?.let {
            if (clock.inFuture(it.lease.expiresAt)) {
                it
            } else {
                null
            }
        }

    fun findRoute(sourceNode: NodeId, targetNode: NodeId): List<NodeId> {
        val graph = nodeGraph.get() ?: buildGraph()

        if (!graph.containsVertex(sourceNode)) {
            logger.debug { "Source node $sourceNode not found in cluster." }
            return emptyList()
        }
        if (!graph.containsVertex(targetNode)) {
            logger.debug { "Target node $targetNode not found in cluster." }
            return emptyList()
        }

        return try {
            val path = DijkstraShortestPath.findPathBetween(graph, sourceNode, targetNode)
            path?.vertexList?.drop(1)
        } catch (e: RuntimeException) {
            logger.debug { "Could not find path between source and target nodes. $e" }
            null
        } ?: emptyList()
    }

    private fun buildGraph(): Graph<NodeId, DefaultEdge> {
        val graph = DefaultDirectedGraph<NodeId, DefaultEdge>(DefaultEdge::class.java)

        val nodes = clusterNodes.values
        nodes.forEach { node ->
            graph.addVertex(node.id)
        }
        nodes.forEach { node ->
            node.visibleNodes.forEach { visibleNode ->
                if (graph.containsVertex(visibleNode)) {
                    graph.addEdge(node.id, visibleNode)
                }
            }
        }

        this.nodeGraph.set(graph)
        return graph
    }
}
