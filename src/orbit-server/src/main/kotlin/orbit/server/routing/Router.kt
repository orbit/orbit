/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.Address
import orbit.server.BaseMessage
import orbit.server.net.NodeId
import orbit.common.collections.GraphTraverser


class Router(
    val nodeId: NodeId,
    val addressableDirectory: AddressableDirectory,
    val nodeDirectory: NodeDirectory,
    val addressablePlacement: AddressablePlacementStrategy
) {
    fun routeMessage(message: BaseMessage, projectedRoute: Route? = null): Route? {
        var lastNode = addressableDirectory.lookup(message.destination) ?: addressablePlacement.chooseNode(message.destination)

        val routeVerified = (projectedRoute != null) && this.verifyRoute(projectedRoute, message.destination)
        println("Finding route between $nodeId -> $lastNode ${if (routeVerified) "(existing)" else ""}")

        val foundRoute = (if (routeVerified) projectedRoute else findRoute(lastNode, message.destination)) ?: return null;

        return if (foundRoute.path.first() == this.nodeId) foundRoute.pop().route else foundRoute
    }

    private fun findRoute(destination: NodeId, address: Address): Route? {

        val nodeRoutes = HashMap<NodeId, Route>()
        val traversal = GraphTraverser<NodeId> { node ->
            nodeDirectory.lookupConnectedNodes(node, address).map { node -> node.id }
        }

        val nodes = traversal.traverse(destination).take(100).mapNotNull { node ->
            val route = nodeRoutes[node.parent]?.push(node.child) ?: Route(listOf(node.child))
            if (nodeRoutes[node.child] == null) {
                nodeRoutes[node.child] = route
                return@mapNotNull route
            }
            return@mapNotNull null
        }

        return nodes.find { r -> r.path.first().equals(this.nodeId) }
    }

    fun verifyRoute(route: Route, address: Address): Boolean {
        var previousNode = this.nodeId
        for (node in route.path.drop(1)) {
            if (!nodeDirectory.lookupConnectedNodes(node, address).any { n -> n.id == previousNode }) {
                return false
            }
            previousNode = node
        }
        return true
    }
}
