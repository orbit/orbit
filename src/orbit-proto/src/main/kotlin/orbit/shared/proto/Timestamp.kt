/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.time.Timestamp

typealias TimestampProto = com.google.protobuf.Timestamp

fun Timestamp.toTimestampProto(): TimestampProto =
    TimestampProto.newBuilder()
        .setSeconds(seconds)
        .setNanos(nanos)
        .build()

fun TimestampProto.toTimestamp() =
    Timestamp(
        seconds = this.seconds,
        nanos = this.nanos
    )