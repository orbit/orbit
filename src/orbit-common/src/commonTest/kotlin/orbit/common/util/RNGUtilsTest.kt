/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RNGUtilsTest {
    @Test
    fun `check random string generates unique ids`() {
        val firstString = RNGUtils.randomString()
        val secondString = RNGUtils.randomString()
        assertTrue { firstString.isNotEmpty() }
        assertTrue { secondString.isNotEmpty() }
        assertNotEquals(firstString, secondString)
    }

    @Test
    fun `check random string generates unique ids 1000 times`() {
        val ids = mutableListOf<String>()
        repeat(1000) {
            ids.add(RNGUtils.randomString())
        }
        assertEquals(ids.size, ids.toSet().size)
    }
}