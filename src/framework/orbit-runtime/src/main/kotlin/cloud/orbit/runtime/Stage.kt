/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime

import cloud.orbit.common.logging.logger
import cloud.orbit.common.util.VersionUtils
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.config.StageConfig
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * The Orbit Stage.
 *
 * This represents a single instance of the Orbit runtime.
 */
class Stage(private val stageConfig: StageConfig) {
    private val logger by logger()
    private val rootScope = SupervisorScope(stageConfig.cpuPool, this::onUnhandledException)

    suspend fun start() {
        rootScope.launch {
            logger.info(
                "Orbit Environment: {} {} {}",
                stageConfig.clusterName,
                stageConfig.nodeIdentity,
                VersionUtils.getVersionInfo()
            )
            logger.info("Starting Orbit...")

            logger.info("Orbit started successfully.")
        }.join()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) {
        logger.error("Unhandled error in Orbit.", throwable)
    }
}