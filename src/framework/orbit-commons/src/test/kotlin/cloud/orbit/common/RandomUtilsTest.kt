/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common

import cloud.orbit.common.util.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RandomUtilsTest {
    @Test
    fun testSecureRandom() {
        val firstString = RandomUtils.secureRandomString()
        val secondString = RandomUtils.secureRandomString()
        assertThat(firstString).isNotEmpty()
        assertThat(secondString).isNotEmpty()
        assertThat(firstString).isNotEqualTo(secondString)
    }

    @Test
    fun testPseudoRandom() {
        val firstString = RandomUtils.pseudoRandomString()
        val secondString = RandomUtils.pseudoRandomString()
        assertThat(firstString).isNotEmpty()
        assertThat(secondString).isNotEmpty()
        assertThat(firstString).isNotEqualTo(secondString)
    }

    @Test
    fun testSequentialId() {
        val first = RandomUtils.sequentialId()
        val second = RandomUtils.sequentialId()
        assertThat(first).isNotEqualTo(second)
    }
}