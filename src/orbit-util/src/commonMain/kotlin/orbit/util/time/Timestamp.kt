/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

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

    companion object
}

expect fun Timestamp.Companion.now(): Timestamp