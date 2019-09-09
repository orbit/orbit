/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.common.collections.GraphTraverser
import orbit.server.net.LocalNodeId
import orbit.server.net.NodeId

internal class Router(
    private val localNode: LocalNodeId,
    private val nodeDirectory: NodeDirectory
) {

    suspend fun getRoute(targetNode: NodeId, projectedRoute: Route? = null): Route? {
        val routeVerified = (projectedRoute != null) && this.verifyRoute(projectedRoute)
        println("Finding route between $localNode -> $targetNode ${if (routeVerified) "(existing)" else ""}")

        val foundRoute =
            (if (routeVerified) projectedRoute else searchRoute(targetNode)) ?: return null;

        return if (foundRoute.nextNode == this.localNode.nodeId) foundRoute.pop().route else foundRoute
    }

    private suspend fun searchRoute(destination: NodeId): Route? {
        val nodeRoutes = HashMap<NodeId, Route>()
        val traversal = GraphTraverser<NodeId> { node ->
            nodeDirectory.lookupConnectedNodes(node).map { n -> n.id }
        }

        val routes = traversal.traverse(destination).take(100).mapNotNull { node ->
            val route = nodeRoutes[node.parent]?.push(node.child) ?: Route(listOf(node.child))
            if (nodeRoutes[node.child] == null) {
                nodeRoutes[node.child] = route
                return@mapNotNull route
            }
            return@mapNotNull null
        }.toList()

        return routes.find { r -> r.nextNode.equals(this.localNode.nodeId) }
    }

    fun verifyRoute(route: Route): Boolean {
//        var previousNode = this.localNode.nodeId
//        for (node in route.path.drop(1)) {
//            if (!nodeDirectory.lookupConnectedNodes(node, address).any { n -> n.id == previousNode }) {
//                return false
//            }
//            previousNode = node
//        }
        return true
    }
}
