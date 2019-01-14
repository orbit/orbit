/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.time

import java.util.concurrent.atomic.AtomicLong

/**
 * A stopwatch for measuring elapsed time.
 */
class Stopwatch private constructor(private val clock: Clock) {
    private val startTime = clock.currentTime
    private val offset = AtomicLong(0)

    /**
     * The amount of time that has elapsed so far.
     */
    val elapsed: Long get() = clock.currentTime - startTime

    companion object {
        /**
         * Starts a stopwatch at the current time.
         */
        @JvmStatic
        fun start(clock: Clock): Stopwatch = Stopwatch(clock)
    }
}