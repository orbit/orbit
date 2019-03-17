/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common

import cloud.orbit.common.util.randomOrNull
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class CollectionUtilsTest {
    @Test
    fun `check randomOrNull null`() {
        val list = listOf<String>()
        val result = list.randomOrNull()
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `check randomOrNull value`() {
        val list = listOf("Hello", "World", "!")
        val result = list.randomOrNull()
        Assertions.assertThat(result).isNotNull().isIn(list)
    }
}