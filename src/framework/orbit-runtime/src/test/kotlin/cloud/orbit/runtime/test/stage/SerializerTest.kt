/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test.stage

import cloud.orbit.core.key.Key
import cloud.orbit.runtime.serialization.Serialization
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WrappedNoKey {
    val noKey: Key.NoKey? = null
}

class SerializerTest : BaseStageTest() {
    private lateinit var serialization: Serialization

    @BeforeEach
    fun setup() {
        serialization = stage.componentProvider.construct()
    }

    @Test
    fun `check NoKey serialized is same instance`() {
        val rawData = serialization.serializeObject(Key.NoKey)
        val newKey: Key.NoKey = serialization.deserializeObject(rawData)
        assertThat(newKey).isSameAs(Key.NoKey)
    }

    @Test
    fun `check NoKey clone is same instance`() {
        val cloned = serialization.cloneObject(Key.NoKey)
        assertThat(cloned).isSameAs(Key.NoKey)
    }

    @Test
    fun `check null NoKey serialized is null`() {
        val rawData = serialization.serializeObject(WrappedNoKey())
        val newKey: WrappedNoKey = serialization.deserializeObject(rawData)
        assertThat(newKey.noKey).isNull()
    }
}