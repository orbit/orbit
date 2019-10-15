/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

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

    /**
     * Advances the internal time by the specified amount.
     *
     * @param offset The amount of time to advance by.
     * @param timeUnit The unit of time to advance by.
     */
    fun advanceTime(offset: TimeMs) {
        offsetTime += offset
    }
}

expect object ClockUtils {
    fun currentTimeMillis(): Long
}

/**
 * Represents a time or duration in milliseconds.
 */
typealias TimeMs = Long