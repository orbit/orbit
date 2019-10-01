/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.time

import java.time.Instant

fun Timestamp.toInstant() = Instant.ofEpochSecond(seconds, nanos.toLong())

fun Instant.toTimestamp(): Timestamp =
    this.toEpochMilli().let { millis ->
        Timestamp(
            seconds = millis / 1000,
            nanos = (millis % 1000 * 1000000).toInt()
        )
    }

actual fun Timestamp.Companion.now(): Timestamp = Instant.now().toTimestamp()