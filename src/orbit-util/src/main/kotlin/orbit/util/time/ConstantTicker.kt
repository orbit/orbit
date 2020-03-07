/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KLogger

class ConstantTicker(
    private val scope: CoroutineScope,
    private val targetTickRate: Long,
    private val clock: Clock,
    private val logger: KLogger? = null,
    private val exceptionHandler: ((Throwable) -> Unit)? = null,
    autoStart: Boolean = false,
    private val onTick: suspend () -> Unit,
    private val onSlowTick: suspend () -> Unit = {}
) {

    private var ticker: Job? = null

    init {
        if (autoStart) start()
    }

    fun start() {
        ticker = scope.launch {
            while (isActive) {
                val (elapsed, _) = stopwatch(clock) {
                    logger?.trace { "Begin tick..." }

                    try {
                        onTick()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        exceptionHandler?.invoke(t) ?: throw t
                    }
                }

                val nextTickDelay = (targetTickRate - elapsed).coerceAtLeast(0)

                if (elapsed > targetTickRate) {
                    logger?.warn {
                        "Slow tick. The application is unable to maintain its tick rate. " +
                                "Last tick took ${elapsed}ms and the reference tick rate is ${targetTickRate}ms. " +
                                "The next tick will take place immediately."
                    }
                    onSlowTick()
                }

                logger?.trace { "Tick completed in ${elapsed}ms. Next tick in ${nextTickDelay}ms." }
                delay(nextTickDelay)
            }
        }
    }

    fun stop() {
        ticker?.cancel()
    }
}