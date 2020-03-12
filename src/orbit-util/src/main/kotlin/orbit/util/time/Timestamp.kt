/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

import java.time.Instant

data class Timestamp(
    val seconds: Long,
    val nanos: Int
) {
    operator fun compareTo(timestamp: Timestamp): Int =
        seconds.compareTo(timestamp.seconds).let { secondCompare ->
            if (secondCompare != 0) {
                secondCompare
            } else {
                nanos.compareTo(timestamp.nanos)
            }
        }

    fun isAfter(time: Instant) = this > time.toTimestamp()
    fun isExactly(time: Instant) = this == time.toTimestamp()

    companion object {
        fun now(): Timestamp = Instant.now().toTimestamp()
    }
}

fun Timestamp.toInstant() = Instant.ofEpochSecond(seconds, nanos.toLong())

fun Instant.toTimestamp(): Timestamp =
    this.toEpochMilli().let { millis ->
        Timestamp(
            seconds = millis / 1000,
            nanos = (millis % 1000 * 1000000).toInt()
        )
    }

