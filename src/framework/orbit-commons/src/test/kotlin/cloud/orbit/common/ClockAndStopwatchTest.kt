/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common

import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.Stopwatch
import cloud.orbit.common.time.stopwatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class ClockAndStopwatchTest {
    @Test
    fun `check advancing the clock`() {
        val advanceTick = 10000L
        val clock = Clock()
        val start = clock.currentTime
        clock.advanceTime(advanceTick)
        val end = clock.currentTime
        assertThat(start).isLessThan(end)
        assertThat(start).isNotCloseTo(end, Offset.offset(advanceTick - 1))
    }

    @Test
    fun `check clock time passes`() {
        val sleepTime = 100L
        val clock = Clock()
        val start = clock.currentTime
        Thread.sleep(sleepTime)
        val end = clock.currentTime
        assertThat(start).isLessThan(end)
        assertThat(start).isNotCloseTo(end, Offset.offset(sleepTime - 1))
    }

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
    fun `check stopwatch wrapper time passes`() {
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