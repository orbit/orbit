/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import orbit.common.concurrent.ShutdownLatch
import orbit.common.logging.logger
import orbit.common.logging.trace
import orbit.common.logging.warn
import orbit.common.util.Clock
import orbit.common.util.stopwatch
import orbit.server.concurrent.RuntimePools
import orbit.server.concurrent.RuntimeScopes
import orbit.server.local.InMemoryAddressableDirectory
import orbit.server.local.InMemoryNodeDirectory
import orbit.server.local.LocalFirstPlacementStrategy
import orbit.server.net.GrpcEndpoint
import orbit.server.net.Message
import orbit.server.net.netModule
import orbit.server.pipeline.Pipeline
import orbit.server.pipeline.pipelineModule
import orbit.server.routing.Route
import orbit.server.routing.Router
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.singleton
import kotlin.coroutines.CoroutineContext

class OrbitServer(private val config: OrbitServerConfig) {
    constructor() : this(OrbitServerConfig())

    private val logger by logger()

    private val nodeDirectory = InMemoryNodeDirectory.Instance
    private val addressableDirectory = InMemoryAddressableDirectory.Instance
    private val loadBalancer = LocalFirstPlacementStrategy(nodeDirectory, addressableDirectory, config.nodeId)
    private val router = Router(config.nodeId, addressableDirectory, nodeDirectory, loadBalancer)

    private var tickJob: Job? = null
    private var shutdownLatch: ShutdownLatch? = null

    private val runtimePools = RuntimePools(
        cpuPool = config.cpuPool,
        ioPool = config.ioPool
    )

    private val runtimeScopes = RuntimeScopes(
        runtimePools = runtimePools,
        exceptionHandler = this::onUnhandledException
    )

    private val basicModule = Kodein.Module(name = "Basic") {
        bind() from singleton { kodein }
        bind() from singleton { kodein.direct }
        bind() from singleton { this@OrbitServer }
        bind() from singleton { config }
        bind() from singleton { runtimePools }
        bind() from singleton { runtimeScopes }
        bind() from singleton { Clock() }
    }

    private val routingModule = Kodein.Module(name = "Routing") {
        bind() from singleton { router }
        bind() from singleton { nodeDirectory }
    }

    private val kodein = Kodein {
        import(basicModule)
        import(routingModule)
        import(pipelineModule)
        import(netModule)
    }

    fun start() = runtimeScopes.cpuScope.launch {
        val clock: Clock by kodein.instance()
        logger.info("Starting Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            onStart()
        }

        if(config.acquireShutdownLatch) shutdownLatch = ShutdownLatch().also { it.acquire() }

        logger.info("Orbit started successfully in {}ms.", elapsed)
    }

    fun stop() = runtimeScopes.cpuScope.launch {
        val clock: Clock by kodein.instance()
        logger.info("Stopping Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            onStop()
        }

        shutdownLatch?.release()

        logger.info("Orbit stopped successfully in {}ms.", elapsed)

    }

    private suspend fun onStart() {
        val pipeline: Pipeline by kodein.instance()
        pipeline.start()

        val grpcEndpoint: GrpcEndpoint by kodein.instance()
        grpcEndpoint.start()

        tickJob = launchTick()
    }

    private suspend fun onTick() {

    }

    private suspend fun onStop() {
        val grpcEndpoint: GrpcEndpoint by kodein.instance()
        grpcEndpoint.stop()

        // Stop the tick
        tickJob?.cancelAndJoin()

        val pipeline: Pipeline by kodein.instance()
        pipeline.stop()
    }

    private fun launchTick() = runtimeScopes.cpuScope.launch {
        val clock: Clock by kodein.instance()
        val targetTickRate = config.tickRate
        while (isActive) {
            val (elapsed, _) = stopwatch(clock) {
                logger.trace { "Begin Orbit tick..." }

                try {
                    onTick()
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    onUnhandledException(coroutineContext, t)
                }
            }

            val nextTickDelay = (targetTickRate - elapsed).coerceAtLeast(0)

            if (elapsed > targetTickRate) {
                logger.warn {
                    "Slow Orbit Tick. The application is unable to maintain its tick rate. " +
                            "Last tick took ${elapsed}ms and the reference tick rate is ${targetTickRate}ms. " +
                            "The next tick will take place immediately."
                }
            }

            logger.trace { "Orbit tick completed in ${elapsed}ms. Next tick in ${nextTickDelay}ms." }
            delay(nextTickDelay)
        }
    }

    internal fun handleMessage(message: Message, projectedRoute: Route? = null) {
        println("handling a message ${message.content}")
        // TODO (brett) - re-integrate routing
//        var route = router.routeMessage(message, projectedRoute)
//        if (route == null) {
//            println("No route found")
//            return
//        }
//
//        val nextNode = route.path.last()
//        val node = nodeDirectory.getNode(nextNode)
//        node?.sendMessage(message, route)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    private fun onUnhandledException(throwable: Throwable) {
        logger.error("Unhandled exception in Orbit.", throwable)
    }
}