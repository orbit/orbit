/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.client.leasing.NodeLeaser
import orbit.client.net.ClientAuthInterceptor
import orbit.client.net.GrpcClient
import orbit.client.net.NodeStatus
import orbit.util.concurrent.SupervisorScope
import orbit.util.di.jvm.ComponentContainer
import orbit.util.time.Clock
import orbit.util.time.ConstantTicker
import orbit.util.time.stopwatch
import kotlin.coroutines.CoroutineContext

class OrbitClient(private val config: OrbitClientConfig = OrbitClientConfig()) {
    private val logger = KotlinLogging.logger { }

    private val container = ComponentContainer()
    private val clock = Clock()

    private val scope = SupervisorScope(
        pool = config.pool,
        exceptionHandler = this::onUnhandledException
    )

    private val ticker = ConstantTicker(
        scope = scope,
        targetTickRate = config.tickRate.toMillis(),
        clock = clock,
        logger = logger,
        exceptionHandler = this::onUnhandledException,
        autoStart = false,
        onTick = this::tick
    )


    init {
        container.configure {
            instance(this@OrbitClient)
            instance(config)
            instance(scope)

            definition<GrpcClient>()
            definition<NodeStatus>()
            definition<ClientAuthInterceptor>()

            definition<NodeLeaser>()

        }
    }

    private val nodeLeaser by container.inject<NodeLeaser>()

    fun start() = scope.launch {
        logger.info("Starting Orbit client...")
        val (elapsed, _) = stopwatch(clock) {
            // Get first lease
            nodeLeaser.joinCluster()

            // Start tick
            ticker.start()
        }

        logger.info("Orbit client started successfully in {}ms.", elapsed)
    }

    private suspend fun tick() {
        nodeLeaser.tick()
    }

    fun stop() = scope.launch {
        logger.info("Stopping Orbit...")
        val (elapsed, _) = stopwatch(clock) {
            // Stop the tick
            ticker.stop()
        }

        logger.info("Orbit stopped successfully in {}ms.", elapsed)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    private fun onUnhandledException(throwable: Throwable) {
        logger.error(throwable) { "Unhandled exception in Orbit Client." }
    }

}