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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import orbit.server.net.NodeId
import orbit.server.pipeline.Pipeline
import orbit.server.pipeline.PipelineContext
import orbit.server.routing.Route
import orbit.server.routing.Router
import org.kodein.di.Kodein
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.singleton
import kotlin.coroutines.CoroutineContext

class OrbitServer(private val config: OrbitConfig) {
    private val logger by logger()

    private val nodeDirectory = InMemoryNodeDirectory()
    private val addressableDirectory = InMemoryAddressableDirectory()
    private val loadBalancer = LocalFirstPlacementStrategy(nodeDirectory, config.nodeId)
    private val router = Router(config.nodeId, addressableDirectory, nodeDirectory, loadBalancer)
    private val grpcEndpoint: GrpcEndpoint = GrpcEndpoint(config, this)

    private var tickJob: Job? = null

    private val runtimePools = RuntimePools(
        cpuPool = config.cpuPool,
        ioPool = config.ioPool
    )

    private val runtimeScopes = RuntimeScopes(
        runtimePools = runtimePools,
        exceptionHandler = this::onUnhandledException
    )

    private val kodein = Kodein {
        bind() from singleton { config }
        bind() from singleton { runtimePools }
        bind() from singleton { runtimeScopes }
        bind() from singleton { Clock() }
        bind() from singleton { Pipeline(instance(), instance()) }

        bind<Iterable<PipelineContext>>() with singleton { listOf<PipelineContext>() }
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
        val pipeline: Pipeline by kodein.instance()
        pipeline.start()

        grpcEndpoint.start()

        tickJob = launchTick()
    }

    private suspend fun onTick() {

    }

    private suspend fun onStop() {
        grpcEndpoint.stop()

        // Stop the tick
        tickJob?.cancelAndJoin()

        val pipeline: Pipeline by kodein.instance()
        pipeline.stop()
    }

    private fun launchTick() = runtimeScopes.cpuScope.launch {
        val clock : Clock by kodein.instance()
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

    fun handleMessage(message: BaseMessage, projectedRoute: Route? = null) {
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