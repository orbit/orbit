/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.numerics.shouldBeLessThan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import orbit.util.time.Clock
import orbit.util.time.highResolutionTicker
import orbit.util.time.stopwatch
import org.junit.Test

@ExperimentalCoroutinesApi
class HighResolutionTickerTest {

    @Test
    fun `Should tick expected number of times`() {
        runBlocking {
            val ticker = highResolutionTicker(10000)

            val watch = stopwatch(Clock()) { _ ->
                (1..10000).forEach { _ ->
                    ticker.receive()
                }
            }

            watch.elapsed shouldBeLessThan 1050
            watch.elapsed shouldBeGreaterThan 950
        }
    }
}
