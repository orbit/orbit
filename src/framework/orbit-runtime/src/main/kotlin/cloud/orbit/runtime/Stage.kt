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
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.config.StageConfig
import cloud.orbit.runtime.di.ComponentProvider
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * The Orbit Stage.
 *
 * This represents a single instance of the Orbit runtime.
 */
class Stage(private val stageConfig: StageConfig) : RuntimeContext {
    private val logger by logger()
    private val rootScope = SupervisorScope(stageConfig.cpuPool, this::onUnhandledException)
    private val componentProvider = ComponentProvider()

    private var tickJob: Job? = null

    override val clock: Clock by componentProvider.inject()
    override val actorProxyFactory: ActorProxyFactory by componentProvider.inject()

    init {
        componentProvider.configure {
            instance<RuntimeContext> { this@Stage }
            instance { this@Stage }
            instance { stageConfig }
            instance { Clock() }
        }
    }

    /**
     * Starts the Orbit stage.
     */
    fun start() = rootScope.start()

    private fun CoroutineScope.start() = async {
        val stopwatch = Stopwatch.start(clock)
        logger.info("Starting Orbit...")

        logEnvironmentInfo()

        logger.info("Orbit started successfully in {}ms.", stopwatch.elapsed)

        tickJob = tickJob()

        Unit
    }

    private fun CoroutineScope.tickJob() = launch {
        val targetTickRate = stageConfig.tickRate
        while (isActive) {
            logger.debug { "Starting Orbit tick..." }
            val stopwatch = Stopwatch.start(clock)

            try {
                tick()
            } catch (t: Throwable) {
                onUnhandledException(coroutineContext, t)
            }

            delay(1234)

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

    private suspend fun tick() {

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