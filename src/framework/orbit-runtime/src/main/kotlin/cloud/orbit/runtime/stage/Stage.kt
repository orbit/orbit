/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.stage

import cloud.orbit.common.logging.*
import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.stopwatch
import cloud.orbit.common.util.VersionUtils
import cloud.orbit.core.actor.ActorProxyFactory
import cloud.orbit.core.net.NodeCapabilities
import cloud.orbit.core.net.NodeInfo
import cloud.orbit.core.net.NodeStatus
import cloud.orbit.core.runtime.RuntimeContext
import cloud.orbit.runtime.actor.DefaultActorProxyFactory
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.NetManager
import cloud.orbit.runtime.pipeline.PipelineManager
import cloud.orbit.runtime.remoting.RemoteInterfaceDefinitionDictionary
import cloud.orbit.runtime.remoting.RemoteInterfaceProxyFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture

/**
 * The Orbit Stage.
 *
 * This represents a single instance of the Orbit runtime.
 */
class Stage(private val stageConfig: StageConfig) : RuntimeContext {
    constructor() : this(StageConfig())

    private val logger by logger()

    private val errorHandler = ErrorHandler()
    private val supervisorScope = SupervisorScope(
        cpuPool = stageConfig.cpuPool,
        ioPool = stageConfig.ioPool,
        exceptionHandler = errorHandler::onUnhandledException
    )
    private val componentProvider = ComponentProvider()
    private val netManager: NetManager by componentProvider.inject()

    private var tickJob: Job? = null

    override val clock: Clock by componentProvider.inject()
    override val actorProxyFactory: ActorProxyFactory by componentProvider.inject()

    init {
        componentProvider.configure {
            // Stage
            instance<RuntimeContext> { this@Stage }
            instance { this@Stage }
            instance { stageConfig }
            instance { supervisorScope }
            instance { errorHandler }

            // Utils
            definition<Clock>()

            // Net
            definition<NetManager>()

            // Remoting
            definition<RemoteInterfaceProxyFactory>()
            definition<RemoteInterfaceDefinitionDictionary>()

            // Pipeline
            definition<PipelineManager>()

            // Capabilities
            definition<CapabilitiesScanner>()

            // Actors
            definition<ActorProxyFactory> { DefaultActorProxyFactory::class.java }
        }

        netManager.localNodeManipulator.replace(
            NodeInfo(
                clusterName = stageConfig.clusterName,
                nodeIdentity = stageConfig.nodeIdentity,
                nodeMode = stageConfig.nodeMode,
                nodeStatus = NodeStatus.STOPPED,
                nodeCapabilities = NodeCapabilities(
                    addressables = listOf()
                )
            )
        )
    }

    /**
     * Starts the Orbit stage.
     */
    fun start() = requestStart().asCompletableFuture()

    /**
     * Stops the Orbit stage
     */
    fun stop() = requestStop().asCompletableFuture()

    private fun requestStart() = supervisorScope.async {
        netManager.localNodeManipulator.updateNodeStatus(NodeStatus.STOPPED, NodeStatus.STARTING)
        logger.info("Starting Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            onStart()
        }
        logger.info("Orbit started successfully in {}ms.", elapsed)

        Unit
    }

    private fun launchTick() = supervisorScope.launch {
        val targetTickRate = stageConfig.tickRate
        while (isActive) {
            val (elapsed, _) = stopwatch(clock) {
                logger.debug { "Begin Orbit tick..." }

                try {
                    onTick()
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    errorHandler.onUnhandledException(coroutineContext, t)
                }
            }

            val nextTickDelay = (targetTickRate - elapsed).coerceAtLeast(0)

            if (elapsed > targetTickRate) {
                logger.warn {
                    "Slow Orbit Tick. The application is unable to maintain its tick rate. " +
                            "Last tick took ${elapsed}ms and the target tick rate is ${targetTickRate}ms. " +
                            "The next tick will take place immediately."
                }
            }

            logger.debug { "Orbit tick completed in ${elapsed}ms. Next tick in ${nextTickDelay}ms." }
            delay(nextTickDelay)
        }
    }

    private fun requestStop() = supervisorScope.async {
        netManager.localNodeManipulator.updateNodeStatus(NodeStatus.RUNNING, NodeStatus.STOPPING)

        logger.info("Orbit stopping...")
        val (elapsed, _) = stopwatch(clock) {
            tickJob?.cancelAndJoin()
            onStop()
        }

        logger.info("Orbit stopped in {}ms.", elapsed)
        Unit
    }

    private suspend fun onStart() {
        val capabilitiesScanner: CapabilitiesScanner by componentProvider.inject()
        val remoteInterfaceDefinitionDictionary: RemoteInterfaceDefinitionDictionary by componentProvider.inject()

        // Log some info about the environment
        logEnvironmentInfo()

        // Determine capabilities
        capabilitiesScanner.scan(*stageConfig.packages.toTypedArray())
        capabilitiesScanner.addressableInterfaces.forEach {
            // Pre populate for performance
            remoteInterfaceDefinitionDictionary.getOrCreate(it)
        }
        val capabilities = capabilitiesScanner.generateNodeCapabilities()
        netManager.localNodeManipulator.updateCapabiltities(capabilities)

        // Flip status to running
        netManager.localNodeManipulator.updateNodeStatus(NodeStatus.STARTING, NodeStatus.RUNNING)

        tickJob = launchTick()
    }

    private suspend fun onTick() {
    }

    private suspend fun onStop() {
        netManager.localNodeManipulator.updateNodeStatus(NodeStatus.STOPPING, NodeStatus.STOPPED)
    }

    private fun logEnvironmentInfo() {
        val versionInfo = VersionUtils.getVersionInfo()
        logger.info {
            "Orbit Environment: ${stageConfig.clusterName} ${stageConfig.nodeIdentity} $versionInfo"
        }

        loggingContext {
            put("orbit.clusterName" to stageConfig.clusterName.value)
            put("orbit.nodeIdentity" to stageConfig.nodeIdentity.value)
            put("orbit.version" to versionInfo.orbitVersion)
            put("orbit.jvmVersion" to versionInfo.jvmVersion)
            put("orbit.jvmBuild" to versionInfo.jvmBuild)
            put("orbit.kotlinVersion" to versionInfo.kotlinVersion)
        }

        logger.trace {
            "Initial Orbit Component Provider State: ${componentProvider.debugString()}"
        }
    }
}