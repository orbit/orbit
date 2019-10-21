/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.router

import com.google.common.graph.GraphBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.NodeDirectory
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.router.Route
import java.time.Instant

@Suppress("UnstableApiUsage")
class Router(private val localNode: LocalNodeInfo, private val nodeDirectory: NodeDirectory) {
    @Suppress("UNUSED_PARAMETER")
    fun findRoute(targetNode: NodeId, possibleRoute: Route? = null): Route {


        return Route(listOf(targetNode))
    }

    var nextUpdate = Instant.now().plusSeconds(10)

    suspend fun tick(scope: CoroutineScope) {
        if (nextUpdate < Instant.now()) {
            buildGraph(scope)
            nextUpdate = Instant.now().plusSeconds(10)
        }
    }

    suspend fun buildGraph(scope: CoroutineScope) {
        println("updating route graph")

        val foundNodes = HashSet<NodeId>()
        val graph = GraphBuilder.undirected().allowsSelfLoops(true).immutable<NodeInfo>()

        fun addNodes(node: NodeInfo) {
            println("calling addNodes ${node.id}")
            graph.addNode(node)
            foundNodes.add(node.id)
            runBlocking {
                node.visibleNodes.filter { n -> !foundNodes.contains(n) }.forEach { n ->
                    val found = nodeDirectory.get(n)
                    if (found != null) {
                        graph.putEdge(node, found)
                        addNodes(found)
                    }
                }
            }
        }
        addNodes(localNode.info)

        println("graph updated")
    }

//    suspend fun getRoute(targetNode: NodeId, projectedRoute: Route? = null): Route? {
//        val routeVerified = (projectedRoute != null) && this.verifyRoute(projectedRoute)
//        println("Finding route between $localNode -> $targetNode ${if (routeVerified) "(existing)" else ""}")
//
//        val foundRoute = (if (routeVerified) projectedRoute else searchRoute(targetNode)) ?: return null;
//
//        return if (foundRoute.nextNode == this.localNode.nodeInfo.id) foundRoute.pop().route else foundRoute
//    }
//
//    private suspend fun searchRoute(destination: NodeId): Route? {
//        val nodeRoutes = HashMap<NodeId, Route>()
//
//        val traversal = GraphTraverser<NodeId> { node ->
//            nodeDirectory.lookupConnectedNodes(node).map { n -> n.id }
//        }
//
//        return traversal.traverse(destination).take(100).mapNotNull { node ->
//            val route = nodeRoutes[node.parent]?.push(node.child) ?: Route(listOf(node.child))
//            if (nodeRoutes[node.child] == null) {
//                nodeRoutes[node.child] = route
//                return@mapNotNull route
//            }
//            return@mapNotNull null
//        }.first { r -> r.nextNode == localNode.nodeInfo.id }
}




