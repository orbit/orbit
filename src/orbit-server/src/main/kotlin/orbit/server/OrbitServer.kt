/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import orbit.common.logging.logger
import orbit.server.local.InMemoryAddressableDirectory
import orbit.server.local.InMemoryNodeDirectory
import orbit.server.local.LocalFirstPlacementStrategy
import orbit.server.net.GrpcEndpoint
import orbit.server.routing.Route
import orbit.server.routing.Router
import org.kodein.di.Kodein
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.singleton
import org.kodein.di.newInstance

class OrbitServer(private val config: OrbitConfig) {
    private val logger by logger()

    private val kodein = Kodein {
        bind<OrbitConfig>() with singleton { config }
        bind<GrpcEndpoint>() with singleton { GrpcEndpoint(instance()) }
    }

    val nodeId = NodeId.generate()
    val nodeDirectory = InMemoryNodeDirectory()
    val addressableDirectory = InMemoryAddressableDirectory()
    val loadBalancer = LocalFirstPlacementStrategy(nodeDirectory, nodeId)
    val router = Router(nodeId, addressableDirectory, nodeDirectory, loadBalancer)

    fun start() {
        logger.info("Starting Orbit...")

        val endpoint: GrpcEndpoint by kodein.instance()

        endpoint.start()

        logger.info("Orbit started.")
    }

    fun handleMessage(message: BaseMessage, projectedRoute: Route? = null) {
        var route = router.routeMessage(message, projectedRoute)
        if (route == null) {
            println("No route found")
            return
        }

        var nextNode = route.path.last()
        var node = nodeDirectory.getNode(nextNode)
        node?.sendMessage(message, route)
    }
}