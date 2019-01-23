/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.time

/**
 * A stopwatch for measuring elapsed time.
 * @param clock The clock to use for measuring time
 */
class Stopwatch private constructor(private val clock: Clock) {
    private val startTime = clock.currentTime

    /**
     * The amount of time that has elapsed so far.
     */
    val elapsed: TimeMs get() = clock.currentTime - startTime

    companion object {
        /**
         * Starts a stopwatch at the current time.
         */
        @JvmStatic
        fun start(clock: Clock): Stopwatch = Stopwatch(clock)
    }
}

/**
 * Represents time elapsed and the result of a computation.
 */
data class ElapsedAndResult<T>(
    /**
     * The time that elapsed in milliseconds.
     */
    val elapsed: TimeMs,
    /**
     * The result of the computation.
     */
    val result: T
)

/**
 * A stopwatch that measures the time that elapses for the given computation.
 *
 * @param clock The clock to use for measuring time.
 * @param body The computation to measure.
 */
inline fun <T> stopwatch(clock: Clock, body: () -> T): ElapsedAndResult<T> {
    val sw = Stopwatch.start(clock)
    val computed = body()
    return ElapsedAndResult(sw.elapsed, computed)
}
