/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import kotlinx.coroutines.launch
import orbit.common.logging.logger
import orbit.common.util.Clock
import orbit.common.util.stopwatch
import orbit.server.concurrent.RuntimePools
import orbit.server.concurrent.RuntimeScopes
import orbit.server.local.InMemoryAddressableDirectory
import orbit.server.local.InMemoryNodeDirectory
import orbit.server.local.LocalFirstPlacementStrategy
import orbit.server.net.GrpcEndpoint
import orbit.server.net.NodeId
import orbit.server.routing.Route
import orbit.server.routing.Router
import org.kodein.di.Kodein
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.singleton
import kotlin.coroutines.CoroutineContext

class OrbitServer(private val config: OrbitConfig) {
    private val logger by logger()

    val nodeDirectory = InMemoryNodeDirectory()
    val addressableDirectory = InMemoryAddressableDirectory()
    val loadBalancer = LocalFirstPlacementStrategy(nodeDirectory, config.nodeId)
    val router = Router(config.nodeId, addressableDirectory, nodeDirectory, loadBalancer)

    private val runtimePools = RuntimePools(
        cpuPool = config.cpuPool,
        ioPool = config.ioPool
    )

    private val runtimeScopes = RuntimeScopes(
        runtimePools = runtimePools,
        exceptionHandler = this::onUnhandledException
    )

    private val kodein = Kodein {
        bind<OrbitConfig>() with singleton { config }
        bind<GrpcEndpoint>() with singleton { GrpcEndpoint(instance()) }
        bind<RuntimePools>() with singleton { runtimePools }
        bind<RuntimeScopes>() with singleton { runtimeScopes }
        bind<Clock>() with singleton { Clock() }
    }

    fun start() = runtimeScopes.ioScope.launch {
        val clock : Clock by kodein.instance()
        logger.info("Starting Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            onStart()
        }
        logger.info("Orbit started successfully in {}ms.", elapsed)

    }

    fun stop() = runtimeScopes.ioScope.launch {
        val clock : Clock by kodein.instance()
        logger.info("Stopping Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            onStop()
        }
        logger.info("Orbit stopped successfully in {}ms.", elapsed)

    }

    private suspend fun onStart() {
        val endpoint: GrpcEndpoint by kodein.instance()

        endpoint.start()
    }

    private suspend fun onStop() {
        val endpoint: GrpcEndpoint by kodein.instance()

        endpoint.stop()
    }

    private fun handleMessage(message: BaseMessage, projectedRoute: Route? = null) {
        var route = router.routeMessage(message, projectedRoute)
        if (route == null) {
            println("No route found")
            return
        }

        val nextNode = route.path.last()
        val node = nodeDirectory.getNode(nextNode)
        node?.sendMessage(message, route)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    private fun onUnhandledException(throwable: Throwable) {
        logger.error("Unhandled exception in Orbit.", throwable)
    }
}