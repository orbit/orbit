/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import java.lang.System.nanoTime

@ExperimentalCoroutinesApi
fun highResolutionTicker(
    ticksPerSecond: Long,
    scope: CoroutineScope = GlobalScope
): ReceiveChannel<Unit> {
    val rate = 1000000000 / ticksPerSecond
    val time: () -> Long = { nanoTime() }

    return scope.produce {
        var count = 0
        val startTime = time()

        while (true) {
            val nextDelay = (startTime + (rate * ++count) - time()).coerceAtLeast(0)
            delay(Math.round(nextDelay / 1000000.0))
            channel.send(Unit)
        }
    }
}
