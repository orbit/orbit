/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import orbit.util.time.Clock
import kotlin.test.Test
import kotlin.test.assertTrue

class ClockTest {
    @Test
    fun `check advancing the clock`() =
        runBlocking {
            val advanceTick = 10000L
            val clock = Clock()
            val start = clock.currentTime
            clock.advanceTime(advanceTick)
            val end = clock.currentTime

            assertTrue { start < end }
        }

    @Test
    fun `check clock time passes`() =
        runBlocking {
            val sleepTime = 100L
            val clock = Clock()
            val start = clock.currentTime

            delay(sleepTime)

            val end = clock.currentTime
            assertTrue { start < end }
        }
}