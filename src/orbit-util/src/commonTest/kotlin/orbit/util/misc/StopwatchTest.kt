/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

import kotlinx.coroutines.delay
import orbit.util.test.runTest
import orbit.util.time.Clock
import orbit.util.time.Stopwatch
import kotlin.test.Test
import kotlin.test.assertTrue

class StopwatchTest {
    @Test
    fun `check stopwatch time passes`() =
        runTest {
            val sleepTime = 100L
            val clock = Clock()
            val stopwatch = Stopwatch.start(clock)
            delay(sleepTime)
            val elapsed = stopwatch.elapsed
            assertTrue { elapsed >= sleepTime }
        }
}