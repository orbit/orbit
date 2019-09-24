/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto.util

import com.google.protobuf.Timestamp
import java.time.Instant

fun Instant.toProto() =
    this.toEpochMilli().let { millis ->
        Timestamp.newBuilder()
            .setSeconds(millis / 1000)
            .setNanos((millis % 1000 * 1000000).toInt())
            .build()
    }

fun Timestamp.toInstant() = Instant.ofEpochSecond(seconds, nanos.toLong())