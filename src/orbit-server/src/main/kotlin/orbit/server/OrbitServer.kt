/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.server.auth.AuthSystem
import orbit.server.concurrent.RuntimePools
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.AddressableDirectory
import orbit.server.mesh.AddressableManager
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.NodeDirectory
import orbit.server.net.ConnectionManager
import orbit.server.net.RemoteMeshNodeManager
import orbit.server.pipeline.Pipeline
import orbit.server.pipeline.PipelineSteps
import orbit.server.pipeline.step.AuthStep
import orbit.server.pipeline.step.BlankStep
import orbit.server.pipeline.step.EchoStep
import orbit.server.pipeline.step.IdentityStep
import orbit.server.pipeline.step.PlacementStep
import orbit.server.pipeline.step.RoutingStep
import orbit.server.pipeline.step.TransportStep
import orbit.server.pipeline.step.VerifyStep
import orbit.server.router.Router
import orbit.server.service.AddressableManagementService
import orbit.server.service.ConnectionService
import orbit.server.service.GrpcEndpoint
import orbit.server.service.HealthCheckList
import orbit.server.service.HealthService
import orbit.server.service.NodeManagementService
import orbit.server.service.ServerAuthInterceptor
import orbit.shared.mesh.NodeStatus
import orbit.util.concurrent.ShutdownLatch
import orbit.util.di.ComponentContainer
import orbit.util.time.Clock
import orbit.util.time.ConstantTicker
import orbit.util.time.stopwatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

class OrbitServer(private val config: OrbitServerConfig) {
    constructor() : this(OrbitServerConfig())

    private val logger = KotlinLogging.logger {}

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
    private val clock = Clock()

    private val grpcEndpoint by container.inject<GrpcEndpoint>()
    private val localNodeInfo by container.inject<LocalNodeInfo>()
    private val nodeManager by container.inject<ClusterManager>()
    private val nodeDirectory by container.inject<NodeDirectory>()
    private val addressableDirectory by container.inject<AddressableDirectory>()

    private val pipeline by container.inject<Pipeline>()
    private val remoteMeshNodeManager by container.inject<RemoteMeshNodeManager>()


    private val ticker = ConstantTicker(
        scope = runtimeScopes.cpuScope,
        targetTickRate = config.tickRate.toMillis(),
        clock = clock,
        logger = logger,
        exceptionHandler = this::onUnhandledException,
        autoStart = false,
        onTick = this::tick
    )

    init {
        container.configure {
            instance(this@OrbitServer)
            instance(config)
            instance(runtimePools)
            instance(runtimeScopes)
            instance(clock)
            instance(config.serverInfo)

            // Service
            definition<GrpcEndpoint>()
            definition<ServerAuthInterceptor>()
            definition<NodeManagementService>()
            definition<AddressableManagementService>()
            definition<ConnectionService>()
            definition<HealthCheckList>()
            definition<HealthService>()

            // Net
            definition<ConnectionManager>()

            // Pipeline
            definition<Pipeline>()
            definition<PipelineSteps>()
            definition<BlankStep>()
            definition<PlacementStep>()
            definition<IdentityStep>()
            definition<RoutingStep>()
            definition<EchoStep>()
            definition<VerifyStep>()
            definition<AuthStep>()
            definition<TransportStep>()

            // Mesh
            definition<LocalNodeInfo>()
            definition<ClusterManager>()
            definition<AddressableManager>()
            definition<RemoteMeshNodeManager>()
            externallyConfigured(config.nodeDirectory)
            externallyConfigured(config.addressableDirectory)
            externallyConfigured(config.meterRegistry)

            // Auth
            definition<AuthSystem>()

            // Router
            definition<Router>()
        }

        Metrics.globalRegistry.add(container.resolve(MeterRegistry::class.java))
    }

    fun start() = runtimeScopes.cpuScope.launch {
        logger.info("Starting Orbit server...")
        val (elapsed, _) = stopwatch(clock) {
            // Start the pipeline
            pipeline.start()

            // Setup  the local node information
            localNodeInfo.start()

            // Start tick
            ticker.start()

            // Start gRPC endpoint
            // We shouldn't do this until we're ready to serve traffic
            grpcEndpoint.start()

            // Flip status to active
            localNodeInfo.updateInfo {
                it.copy(nodeStatus = NodeStatus.ACTIVE)
            }

            // Acquire the latch
            if (config.acquireShutdownLatch) {
                ShutdownLatch().also {
                    shutdownLatch.set(it)
                    it.acquire()
                }
            }
        }

        logger.info("Orbit server started successfully in {}ms.", elapsed)
    }

    fun stop() = runtimeScopes.cpuScope.launch {
        logger.info("Stopping Orbit server...")
        val (elapsed, _) = stopwatch(clock) {
            // Flip status to draining
            localNodeInfo.updateInfo {
                it.copy(nodeStatus = NodeStatus.DRAINING)
            }

            // Stop gRPC
            val grpcEndpoint by container.inject<GrpcEndpoint>()
            grpcEndpoint.stop()

            // Stop the tick
            ticker.stop()

            // Stop pipeline
            pipeline.stop()

            // Flip status to draining
            localNodeInfo.updateInfo {
                it.copy(nodeStatus = NodeStatus.STOPPED)
            }

            // Release the latch
            shutdownLatch.get()?.release().also {
                shutdownLatch.set(null)
            }
        }

        logger.info("Orbit server stopped successfully in {}ms.", elapsed)
    }

    private suspend fun tick() {
        localNodeInfo.tick()

        nodeManager.tick()

        nodeDirectory.tick()

        addressableDirectory.tick()

        remoteMeshNodeManager.tick()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    private fun onUnhandledException(throwable: Throwable) {
        logger.error(throwable) { "Unhandled exception in Orbit." }
    }
}