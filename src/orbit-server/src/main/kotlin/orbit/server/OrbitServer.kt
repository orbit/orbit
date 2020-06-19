/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import orbit.server.service.Meters
import orbit.server.service.NodeManagementService
import orbit.server.service.ServerAuthInterceptor
import orbit.shared.mesh.NodeStatus
import orbit.util.concurrent.ShutdownLatch
import orbit.util.di.ComponentContainer
import orbit.util.instrumentation.recordSuspended
import orbit.util.time.ConstantTicker
import orbit.util.time.stopwatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

class OrbitServer(private val config: OrbitServerConfig) {

    val nodeStatus: NodeStatus get() = localNodeInfo.info.nodeStatus

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
    private val clock = config.clock

    private val grpcEndpoint by container.inject<GrpcEndpoint>()
    private val localNodeInfo by container.inject<LocalNodeInfo>()
    private val clusterManager by container.inject<ClusterManager>()
    private val nodeDirectory by container.inject<NodeDirectory>()
    private val addressableDirectory by container.inject<AddressableDirectory>()

    private val pipeline by container.inject<Pipeline>()
    private val remoteMeshNodeManager by container.inject<RemoteMeshNodeManager>()

    private val slowTick = Metrics.counter(Meters.Names.SlowTicks)
    private val tickTimer = Metrics.timer(Meters.Names.TickTimer)

    private val ticker = ConstantTicker(
        scope = runtimeScopes.cpuScope,
        targetTickRate = config.tickRate.toMillis(),
        clock = clock,
        logger = logger,
        exceptionHandler = this::onUnhandledException,
        autoStart = false,
        onTick = this::tick,
        onSlowTick = { slowTick.increment() }
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
            singleton<GrpcEndpoint>()
            singleton<ServerAuthInterceptor>()
            singleton<NodeManagementService>()
            singleton<AddressableManagementService>()
            singleton<ConnectionService>()
            singleton<HealthCheckList>()
            singleton<HealthService>()

            // Net
            singleton<ConnectionManager>()

            // Pipeline
            singleton<Pipeline>()
            singleton<PipelineSteps>()
            singleton<BlankStep>()
            singleton<PlacementStep>()
            singleton<IdentityStep>()
            singleton<RoutingStep>()
            singleton<EchoStep>()
            singleton<VerifyStep>()
            singleton<AuthStep>()
            singleton<TransportStep>()

            // Mesh
            singleton<LocalNodeInfo>()
            singleton<ClusterManager>()
            singleton<AddressableManager>()
            singleton<RemoteMeshNodeManager>()
            externallyConfigured(config.nodeDirectory)
            externallyConfigured(config.addressableDirectory)
            externallyConfigured(config.meterRegistry)

            // Auth
            singleton<AuthSystem>()

            // Router
            singleton<Router>()

            // Hook to allow overriding container definitions
            config.containerOverrides(this)
        }

        Metrics.globalRegistry.add(container.resolve(MeterRegistry::class.java))

        Metrics.gauge(Meters.Names.AddressableCount, addressableDirectory) { d -> runBlocking { d.count().toDouble() } }
        Metrics.gauge(Meters.Names.NodeCount, nodeDirectory) { d -> runBlocking { d.keys().count().toDouble() } }
    }

    fun start() = runtimeScopes.cpuScope.launch {
        logger.info("Starting Orbit server...")
        logger.info("Lease expirations: Addressable: ${config.addressableLeaseDuration.leaseDuration}s, Node: ${config.nodeLeaseDuration.leaseDuration}s")
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
        Metrics.globalRegistry.remove(container.resolve(MeterRegistry::class.java))
    }

    private suspend fun tick() {
        tickTimer.recordSuspended {
            localNodeInfo.tick()
            clusterManager.tick()
            nodeDirectory.tick()
            addressableDirectory.tick()
            remoteMeshNodeManager.tick()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    private fun onUnhandledException(throwable: Throwable) {
        logger.error(throwable) { "Unhandled exception in Orbit." }
    }
}
