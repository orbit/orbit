/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StopwatchTest {
    @Test
    fun `check stopwatch time passes`() {
        val sleepTime = 100L
        val clock = Clock()
        val stopwatch = Stopwatch.start(clock)
        Thread.sleep(sleepTime)
        val elapsed = stopwatch.elapsed
        assertThat(elapsed).isGreaterThanOrEqualTo(sleepTime)
    }

    @Test
    fun `check stopwatch basic wrapper time passes`() {
        val sleepTime = 100L
        val clock = Clock()
        val (elapsed, _) = stopwatch(clock) {
            Thread.sleep(sleepTime)
        }
        assertThat(elapsed).isGreaterThanOrEqualTo(sleepTime)
    }

    @Test
    fun `check stopwatch suspending wrapper time passes`() {
        runBlocking {
            val sleepTime = 100L
            val clock = Clock()
            val (elapsed, _) = stopwatch(clock) {
                delay(sleepTime)
            }
            assertThat(elapsed).isGreaterThanOrEqualTo(sleepTime)
        }
    }
}