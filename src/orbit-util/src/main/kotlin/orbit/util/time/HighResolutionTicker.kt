/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
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
        var count = 1
        val startTime = time()
        var deadline = startTime + rate

        while (true) {
            val now = time()
            val nextDelay = (deadline - now).coerceAtLeast(0)
            delay(nextDelay / 1000000)
            channel.send(Unit)
            deadline = startTime + (rate * count++)
        }
    }
}
