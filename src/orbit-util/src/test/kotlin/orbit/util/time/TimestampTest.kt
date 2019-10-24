/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.time

import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimestampTest {
    @Test
    fun `test timestamp comparison`() {
        val first = Timestamp.now()
        Thread.sleep(100)
        val later = Timestamp.now()

        assertEquals(first, first)
        assertTrue { first < later }
        assertTrue { later > first }
    }

    @Test
    fun `test timestamp conversion to instant`() {
        val initial = Timestamp.now()
        val converted = initial.toInstant()
        val final = converted.toTimestamp()
        assertEquals(initial, final)
    }

    @Test
    fun `test instant conversion to timestamp`() {
        val initial = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val converted = initial.toTimestamp()
        val final = converted.toInstant()
        assertEquals(initial, final)
    }
}