/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RNGUtilsTest {
    @Test
    fun `check secure random generates unique ids`() {
        val firstString = RNGUtils.secureRandomString()
        val secondString = RNGUtils.secureRandomString()
        assertThat(firstString).isNotEmpty()
        assertThat(secondString).isNotEmpty()
        assertThat(firstString).isNotEqualTo(secondString)
    }

    @Test
    fun `check secure random generates unique ids 1000 times`() {
        val ids = mutableListOf<String>()
        repeat(1000) {
            ids.add(RNGUtils.secureRandomString())
        }

        assertThat(ids.size).isEqualTo(ids.toSet().size)
    }

    @Test
    fun `check pseudo random generates unique ids`() {
        val firstString = RNGUtils.pseudoRandomString()
        val secondString = RNGUtils.pseudoRandomString()
        assertThat(firstString).isNotEmpty()
        assertThat(secondString).isNotEmpty()
        assertThat(firstString).isNotEqualTo(secondString)
    }
}