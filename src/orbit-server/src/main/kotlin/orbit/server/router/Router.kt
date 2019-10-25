/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.router

import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.NodeDirectory
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.router.Route
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@Suppress("UnstableApiUsage")
class Router(private val localNode: LocalNodeInfo, private val clusterManager: ClusterManager) {
    private val graph = AtomicReference<ImmutableGraph<NodeInfo>>()

    @Suppress("UNUSED_PARAMETER")
    suspend fun findRoute(targetNode: NodeId, possibleRoute: Route? = null): Route {
        val path = traverse(targetNode)
        return Route(path.toList())
    }

    suspend fun traverse(targetNode: NodeId): Iterable<NodeId> {
        val graph = this.graph.get() ?: buildGraph()
        val root = graph.nodes().find { n -> n.id == localNode.info.id }!!

        val queue = LinkedList<NodeInfo>(graph.nodes())
        val dist = HashMap<NodeId, Int>()
        val prev = HashMap<NodeId, NodeId>()

        dist[root.id] = 0

        while (queue.any()) {
            val next = queue.minBy { n -> dist[n.id] ?: Int.MAX_VALUE }!!
            queue.remove(next)

            if (next.id == targetNode) {
                fun unroll(node: NodeId): Iterable<NodeId> {
                    if (prev[node] != null) {
                        return unroll(prev[node]!!).plus(node)
                    }
                    return emptyList()
                }
                return unroll(targetNode)
            }

            graph.adjacentNodes(next).forEach {
                val len = (dist[next.id] ?: 0) + 1
                if (len < dist[it.id] ?: Int.MAX_VALUE) {
                    dist[it.id] = len
                    prev[it.id] = next.id
                }
            }
        }
        return emptyList()
    }

    var nextUpdate = Instant.now().plusSeconds(10)

    suspend fun tick() {
        if (nextUpdate < Instant.now()) {
            buildGraph()
            nextUpdate = Instant.now().plusSeconds(10)
        }
    }

    suspend fun buildGraph(): ImmutableGraph<NodeInfo> {
        val foundNodes = HashSet<NodeId>()
        val graph = GraphBuilder.undirected().allowsSelfLoops(true).immutable<NodeInfo>()

        fun addNodes(node: NodeInfo) {
            graph.addNode(node)
            foundNodes.add(node.id)
            runBlocking {
                node.visibleNodes.filter { n -> !foundNodes.contains(n) }.forEach { n ->
                    val found = clusterManager.getNode(n)
                    if (found != null) {
                        graph.putEdge(node, found)
                        addNodes(found)
                    }
                }
            }
        }
        addNodes(localNode.info)

        this.graph.set(graph.build())
        return this.graph.get()
    }
}




