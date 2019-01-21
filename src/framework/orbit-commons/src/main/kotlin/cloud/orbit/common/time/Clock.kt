/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.time

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * A clock used to measure time.
 */
class Clock {
    private val offsetTime = AtomicLong(0)

    /**
     * Gets the current milliseconds since the epoch according to this clock.
     * Typically this is the same as from the JVM but may be different if the time has been manipulated.
     *
     * @return The current timestamp.
     */
    val currentTime: TimeMs get() = System.currentTimeMillis() + offsetTime.get()

    /**
     * Advances the internal time by the specified amount.
     *
     * @param offset The amount of time to advance by.
     * @param timeUnit The unit of time to advance by.
     */
    @JvmOverloads
    fun advanceTime(offset: TimeMs, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
        val toAdd = timeUnit.toMillis(offset)
        offsetTime.addAndGet(toAdd)
    }
}

/**
 * Represents a time or duration in milliseconds.
 */
typealias TimeMs = Long