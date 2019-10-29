/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.server.OrbitServerConfig
import orbit.shared.exception.InvalidChallengeException
import orbit.shared.exception.InvalidNodeId
import orbit.shared.mesh.ChallengeToken
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeLease
import orbit.util.misc.RNGUtils
import orbit.util.time.Timestamp
import orbit.util.time.toTimestamp
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ClusterManager(
    config: OrbitServerConfig,
    private val nodeDirectory: NodeDirectory
) {
    private val leaseExpiration = config.nodeLeaseDuration
    private val clusterNodes = ConcurrentHashMap<NodeId, NodeInfo>()
    private val nodeGraph = AtomicReference<Graph<NodeId, DefaultEdge>>()

    fun getAllNodes() =
        clusterNodes.filter { it.value.lease.expiresAt.inFuture() }.values

    fun getGraph(): Graph<NodeId, DefaultEdge> = nodeGraph.get()

    suspend fun tick() {
        val allNodes = nodeDirectory.entries()
        clusterNodes.clear()
        clusterNodes.putAll(allNodes)
        buildGraph()
    }

    suspend fun joinCluster(namespace: String, capabilities: NodeCapabilities, url: String? = null): NodeInfo {
        do {
            val newNodeId = NodeId.generate(namespace)

            val lease = NodeLease(
                challengeToken = RNGUtils.randomString(64),
                expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
            )

            val info = NodeInfo(
                id = newNodeId,
                capabilities = capabilities,
                lease = lease,
                url = url
            )

            if (nodeDirectory.compareAndSet(newNodeId, null, info)) {
                tick()
                return info
            }
        } while (true)
    }

    suspend fun renewLease(nodeId: NodeId, challengeToken: ChallengeToken, capabilities: NodeCapabilities): NodeInfo =
        updateNode(nodeId) { initialValue ->
            if (initialValue == null || Timestamp.now() > initialValue.lease.expiresAt) {
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
                )
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
            if (it.lease.expiresAt.inFuture()) {
                it
            } else {
                null
            }
        }


    private fun buildGraph() {
        val g = DefaultDirectedGraph<NodeId, DefaultEdge>(DefaultEdge::class.java)

        val nodes = clusterNodes.values
        nodes.forEach { node ->
            g.addVertex(node.id)
        }
        nodes.forEach { node ->
            node.visibleNodes.forEach { visibleNode ->
                g.addEdge(node.id, visibleNode)
            }
        }

        this.nodeGraph.set(g)
    }
}
