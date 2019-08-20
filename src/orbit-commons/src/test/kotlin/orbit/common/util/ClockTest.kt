/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class ClockTest {
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
}