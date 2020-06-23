/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

import java.time.Instant

/**
 * A clock used to measure time.
 */
class Clock {
    private var offsetTime = 0L

    /**
     * Gets the current milliseconds since the epoch according to this clock.
     * Typically this is the same as from the JVM but may be different if the time has been manipulated.
     *
     * @return The current timestamp.
     */
    val currentTime: TimeMs get() = ClockUtils.currentTimeMillis() + offsetTime

    fun now() = Instant.ofEpochMilli(currentTime)

    /**
     * Advances the internal time by the specified amount.
     *
     * @param offset The amount of time to advance by.
     * @param timeUnit The unit of time to advance by.
     */
    fun advanceTime(offset: TimeMs) {
        offsetTime += offset
    }

    /**
     * Resets clock to the current time removing any offsets from advancing the clock manually
     */
    fun resetToNow() {
        offsetTime = 0L
    }

    fun inFuture(time: Timestamp) = time.isAfter(now())
    fun inPast(time: Timestamp) = !inFuture(time)
    fun nowOrPast(time: Timestamp) = time.isExactly(now()) || inPast(time)
}

object ClockUtils {
    fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * Represents a time or duration in milliseconds.
 */
typealias TimeMs = Long