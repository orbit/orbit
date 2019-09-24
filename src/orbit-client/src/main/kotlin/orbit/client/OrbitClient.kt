/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import orbit.client.leasing.NodeLeaser
import orbit.client.net.AuthInterceptor
import orbit.client.net.GrpcClient
import orbit.client.net.ConnectionHandler
import orbit.client.net.NodeStatus
import orbit.common.concurrent.SupervisorScope
import orbit.common.di.ComponentProvider
import orbit.common.logging.logger
import orbit.common.logging.trace
import orbit.common.logging.warn
import orbit.common.util.Clock
import orbit.common.util.stopwatch
import kotlin.coroutines.CoroutineContext

class OrbitClient(private val config: OrbitClientConfig) {
    constructor() : this(OrbitClientConfig())

    private val container = ComponentProvider()
    private val logger by logger()

    private val scope = SupervisorScope(
        pool = config.pool,
        exceptionHandler = this::onUnhandledException
    )

    private val nodeLeaser by container.inject<NodeLeaser>()
    private val connectionHandler by container.inject<ConnectionHandler>()


    private var tickJob: Job? = null

    init {
        container.configure {
            instance(this@OrbitClient)
            instance(config)

            definition<Clock>()

            definition<NodeStatus>()
            definition<GrpcClient>()
            definition<NodeLeaser>()
            definition<AuthInterceptor>()

            definition<ConnectionHandler>()

        }
    }

    fun start() = scope.launch {
        val clock: Clock by container.inject()
        logger.info("Starting Orbit client...")
        val (elapsed, _) = stopwatch(clock) {
            onStart()
            launchTick()
        }

        logger.info("Orbit client started successfully in {}ms.", elapsed)
    }

    fun stop() = scope.launch {
        val clock: Clock by container.inject()
        logger.info("Stopping Orbit client...")
        val (elapsed, _) = stopwatch(clock) {
            onStop()
        }

        logger.info("Orbit client stopped successfully in {}ms.", elapsed)

    }

    private suspend fun onStart() {
        nodeLeaser.joinCluster()
        connectionHandler.connect()
    }

    private suspend fun tick() {
        nodeLeaser.tick()
    }

    private suspend fun onStop() {
        connectionHandler.disconnect()
    }

    private fun launchTick() = scope.launch {
        val clock: Clock by container.inject()
        val targetTickRate = config.tickRate.toMillis()
        while (isActive) {
            val (elapsed, _) = stopwatch(clock) {
                logger.trace { "Begin Orbit client tick..." }

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
                    "Slow Orbit client tick. The application is unable to maintain its tick rate. " +
                            "Last tick took ${elapsed}ms and the reference tick rate is ${targetTickRate}ms. " +
                            "The next tick will take place immediately."
                }
            }

            logger.trace { "Orbit client tick completed in ${elapsed}ms. Next tick in ${nextTickDelay}ms." }
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