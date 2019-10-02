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
import mu.KotlinLogging
import orbit.common.concurrent.jvm.ShutdownLatch
import orbit.common.di.jvm.ComponentContainer
import orbit.common.util.Clock
import orbit.common.util.stopwatch
import orbit.server.concurrent.RuntimePools
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.NodeDirectory
import orbit.server.mesh.ClusterManager
import orbit.server.net.ConnectionManager
import orbit.server.pipeline.Pipeline
import orbit.server.service.ConnectionService
import orbit.server.service.GrpcEndpoint
import orbit.server.service.NodeManagementService
import orbit.server.service.ServerAuthInterceptor
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

class OrbitServer(private val config: OrbitServerConfig) {
    constructor() : this(OrbitServerConfig())

    private val logger = KotlinLogging.logger {}

    private var tickJob = AtomicReference<Job>()
    private var shutdownLatch = AtomicReference<ShutdownLatch>()

    private val runtimePools = RuntimePools(
        cpuPool = config.cpuPool,
        ioPool = config.ioPool
    )

    private val runtimeScopes = RuntimeScopes(
        runtimePools = runtimePools,
        exceptionHandler = this::onUnhandledException
    )

    private val container = ComponentContainer()

    private val clock: Clock by container.inject()
    private val grpcEndpoint by container.inject<GrpcEndpoint>()
    private val localNodeInfo by container.inject<LocalNodeInfo>()
    private val nodeManager by container.inject<ClusterManager>()
    private val nodeDirectory by container.inject<NodeDirectory>()
    private val pipeline by container.inject<Pipeline>()

    init {
        container.configure {
            instance(this@OrbitServer)
            instance(config)
            instance(runtimePools)
            instance(runtimeScopes)
            definition<Clock>()

            // Service
            definition<GrpcEndpoint>()
            definition<ServerAuthInterceptor>()
            definition<NodeManagementService>()
            definition<ConnectionService>()

            // Net
            definition<ConnectionManager>()

            // Pipeline
            definition<Pipeline>()

            // Mesh
            definition<LocalNodeInfo>()
            definition<ClusterManager>()
            externallyConfigured(config.nodeDirectory)
        }
    }

    fun start() = runtimeScopes.cpuScope.launch {
        logger.info("Starting Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            // Start the pipeline
            pipeline.start()

            // Setup  the local node information
            localNodeInfo.start()

            // Start the tick
            tickJob.set(launchTick())

            // Start gRPC endpoint
            // We shouldn't do this until we're ready to serve traffic
            grpcEndpoint.start()

            // Acquire the latch
            if (config.acquireShutdownLatch) {
                ShutdownLatch().also {
                    shutdownLatch.set(it)
                    it.acquire()
                }
            }
        }

        logger.info("Orbit started successfully in {}ms.", elapsed)
    }

    fun stop() = runtimeScopes.cpuScope.launch {
        logger.info("Stopping Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            // Stop gRPC
            val grpcEndpoint by container.inject<GrpcEndpoint>()
            grpcEndpoint.start()

            // Stop the tick
            tickJob.get()?.cancelAndJoin().also {
                tickJob.set(null)
            }

            // Stop pipeline
            pipeline.stop()

            // Release the latch
            shutdownLatch.get()?.release().also {
                shutdownLatch.set(null)
            }
        }

        logger.info("Orbit stopped successfully in {}ms.", elapsed)
    }

    suspend fun tick() {
        // Update the local node info
        localNodeInfo.tick()

        // Node manager
        nodeManager.tick()

        // Tick the node directory
        nodeDirectory.tick()
    }

    private fun launchTick() = runtimeScopes.cpuScope.launch {
        val targetTickRate = config.tickRate.toMillis()
        while (isActive) {
            val (elapsed, _) = stopwatch(clock) {
                logger.trace { "Begin Orbit tick..." }

                try {
                    tick()
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
        logger.error(throwable) { "Unhandled exception in Orbit." }
    }
}