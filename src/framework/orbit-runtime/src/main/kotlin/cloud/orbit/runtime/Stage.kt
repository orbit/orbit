/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime

import cloud.orbit.common.logging.*
import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.Stopwatch
import cloud.orbit.common.util.VersionUtils
import cloud.orbit.core.actor.ActorProxyFactory
import cloud.orbit.core.runtime.RuntimeContext
import cloud.orbit.runtime.actor.DefaultActorProxyFactory
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.config.StageConfig
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.pipeline.PipelineManager
import cloud.orbit.runtime.remoting.RemoteInterfaceDefinitionDictionary
import cloud.orbit.runtime.remoting.RemoteInterfaceProxyFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlin.coroutines.CoroutineContext

/**
 * The Orbit Stage.
 *
 * This represents a single instance of the Orbit runtime.
 */
class Stage(private val stageConfig: StageConfig) : RuntimeContext {
    private val logger by logger()
    private val supervisorScope = SupervisorScope(stageConfig.cpuPool, this::onUnhandledException)
    private val componentProvider = ComponentProvider()

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

            // Utils
            instance { Clock() }

            // Remoting
            definition<RemoteInterfaceProxyFactory>()
            definition<RemoteInterfaceDefinitionDictionary>()

            // Pipeline
            definition<PipelineManager>()

            // Actors
            definition<ActorProxyFactory> { DefaultActorProxyFactory::class.java }
        }
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
        onStart()
        Unit
    }

    private fun launchTick() = supervisorScope.launch {
        val targetTickRate = stageConfig.tickRate
        while (isActive) {
            val stopwatch = Stopwatch.start(clock)
            logger.debug { "Begin Orbit tick..." }

            try {
                onTick()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                onUnhandledException(coroutineContext, t)
            }


            val elapsed = stopwatch.elapsed
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
        logger.info("Orbit stop request received.")
        tickJob?.cancelAndJoin()
        onStop()
        Unit
    }

    private suspend fun onStart() {
        val stopwatch = Stopwatch.start(clock)
        logger.info("Starting Orbit...")

        logEnvironmentInfo()

        logger.info("Orbit started successfully in {}ms.", stopwatch.elapsed)

        tickJob = launchTick()
    }

    private suspend fun onTick() {

    }

    private suspend fun onStop() {
        val stopwatch = Stopwatch.start(clock)
        logger.info("Stopping Orbit...")

        logger.info("Orbit stopped in {}ms.", stopwatch.elapsed)
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

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) {
        logger.error("Unhandled exception in Orbit.", throwable)
    }
}