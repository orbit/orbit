/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime

import cloud.orbit.common.logging.info
import cloud.orbit.common.logging.logger
import cloud.orbit.common.logging.loggingContext
import cloud.orbit.common.logging.trace
import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.Stopwatch
import cloud.orbit.common.util.VersionUtils
import cloud.orbit.core.actor.ActorProxyFactory
import cloud.orbit.core.runtime.RuntimeContext
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.config.StageConfig
import cloud.orbit.runtime.di.ComponentProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

    fun startAsync(): Job = rootScope.launch {
        val stopwatch = Stopwatch.start(clock)
        logger.info("Starting Orbit...")

        logEnvironmentInfo()

        logger.info("Orbit started successfully in {}ms.", stopwatch.elapsed)
    }

    suspend fun start() {
        startAsync().join()
    }

    private fun logEnvironmentInfo() {
        val versionInfo = VersionUtils.getVersionInfo()
        logger.info {
            "Orbit Environment: ${stageConfig.clusterName} ${stageConfig.nodeIdentity} $versionInfo"
        }

        loggingContext {
            put("orbit.clusterName" to stageConfig.clusterName.value)
            put("orbit.nodeIdentity" to  stageConfig.nodeIdentity.value)
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