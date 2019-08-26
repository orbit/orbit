/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.collections

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CollectionExtensionsTest {
    @Test
    fun `check randomOrNull returns value`() {
        val myList = listOf("one", "two")
        val returnValue = myList.randomOrNull()
        assertThat(returnValue).isNotNull()
        assertThat(myList).contains(returnValue)
    }

    @Test
    fun `check randomOrNull returns null when empty`() {
        val myList = listOf<String>()
        val returnValue = myList.randomOrNull()
        assertThat(returnValue).isNull()
    }
}