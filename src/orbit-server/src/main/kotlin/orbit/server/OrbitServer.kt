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
import orbit.server.net.ClientConnections
import orbit.server.net.GrpcEndpoint
import orbit.server.net.MeshConnections
import orbit.server.net.NodeCollection
import orbit.server.pipeline.Pipeline
import orbit.server.pipeline.steps.AddressablePipelineStep
import orbit.server.pipeline.steps.BlankPipelineStep
import orbit.server.pipeline.steps.PipelineSteps
import orbit.server.pipeline.steps.RoutingPipelineStep
import orbit.server.routing.AddressableDirectory
import orbit.server.routing.AddressablePlacementStrategy
import orbit.server.routing.NodeDirectory
import orbit.server.routing.Router
import kotlin.coroutines.CoroutineContext

class OrbitServer(private val config: OrbitServerConfig) {
    constructor() : this(OrbitServerConfig())

    private val logger by logger()

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

    private val container = cloud.orbit.runtime.di.ComponentProvider()

    init {
        container.configure {
            instance(config.localNode)
            instance(this@OrbitServer)
            instance(config)
            instance(runtimePools)
            instance(runtimeScopes)
            definition<Clock>()

            definition<Router>()
            definition<NodeDirectory>(InMemoryNodeDirectory::class.java)
            definition<AddressableDirectory>(InMemoryAddressableDirectory::class.java)
            definition<AddressablePlacementStrategy>(LocalFirstPlacementStrategy::class.java)

            definition<MeshConnections>()
            definition<ClientConnections>()
            definition<NodeCollection>()

            definition<GrpcEndpoint>()

            definition<Pipeline>()
            definition<BlankPipelineStep>()
            definition<AddressablePipelineStep>()
            definition<RoutingPipelineStep>()
            definition<PipelineSteps>()
        }
    }

    fun start() = runtimeScopes.cpuScope.launch {
        val clock: Clock by container.inject()
        logger.info("Starting Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            onStart()
        }

        if (config.acquireShutdownLatch) shutdownLatch = ShutdownLatch().also { it.acquire() }

        logger.info("Orbit started successfully in {}ms.", elapsed)
    }

    fun stop() = runtimeScopes.cpuScope.launch {
        val clock: Clock by container.inject()
        logger.info("Stopping Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            onStop()
        }

        shutdownLatch?.release()

        logger.info("Orbit stopped successfully in {}ms.", elapsed)

    }

    private suspend fun onStart() {
        val pipeline: Pipeline by container.inject()
        pipeline.start()

        val grpcEndpoint: GrpcEndpoint by container.inject()
        grpcEndpoint.start()

        tickJob = launchTick()
        val nodeDirectory: NodeDirectory by container.inject()
        nodeDirectory.connectNode(
            NodeDirectory.NodeInfo(
                config.localNode.nodeId,
                host = "0.0.0.0",
                port = config.grpcPort
            )
        )

        val meshConnections: MeshConnections by container.inject()
        meshConnections.start()
    }

    private suspend fun onTick() {

    }

    private suspend fun onStop() {
        val grpcEndpoint: GrpcEndpoint by container.inject()
        grpcEndpoint.stop()

        // Stop the tick
        tickJob?.cancelAndJoin()

        val pipeline: Pipeline by container.inject()
        pipeline.stop()
    }

    private fun launchTick() = runtimeScopes.cpuScope.launch {
        val clock: Clock by container.inject()
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

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    private fun onUnhandledException(throwable: Throwable) {
        logger.error("Unhandled exception in Orbit.", throwable)
    }
}