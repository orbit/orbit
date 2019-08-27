/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.common.collections.GraphTraverser
import orbit.server.Address
import orbit.server.Capability
import orbit.server.net.Message
import orbit.server.net.MessageContent
import orbit.server.net.NodeId

internal class Router(
    val nodeId: NodeId,
    val addressableDirectory: AddressableDirectory,
    val nodeDirectory: NodeDirectory,
    val addressablePlacement: AddressablePlacementStrategy
) : MeshNode {
    override val id = nodeId

    init {
        nodeDirectory.connectNode(this)
    }

    override val capabilities: List<Capability>
        get() = listOf()

    override fun <T : Address> canHandle(address: T) = true

    override fun sendMessage(message: Message, route: Route?) {
        val route = this.getRoute(message, route)

        if (route == null) {
            println("No route found")
            return
        }

        val nextNode = route.path.last()
        val node = nodeDirectory.getNode(nextNode)
        node?.sendMessage(message, route)
    }

    fun getRoute(message: Message, projectedRoute: Route? = null): Route? {
        println("~| ${message.content}")
        val destination = (message.content as MessageContent.Request).destination!!

        var lastNode = addressableDirectory.lookup(destination) ?: addressablePlacement.chooseNode(destination)

        val routeVerified = (projectedRoute != null) && this.verifyRoute(projectedRoute, destination)
        println("Finding route between $nodeId -> $lastNode ${if (routeVerified) "(existing)" else ""}")

        val foundRoute =
            (if (routeVerified) projectedRoute else searchRoute(lastNode, destination)) ?: return null;

        return if (foundRoute.path.first() == this.nodeId) foundRoute.pop().route else foundRoute
    }

    private fun searchRoute(destination: NodeId, address: Address): Route? {
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
        }.toList()

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
