/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.common.time.Clock
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.special.capabilities.basic.BasicCapabilities
import cloud.orbit.runtime.special.capabilities.basic.BasicCapabilitiesActor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test


class CapabilitiesTest {
    @Test
    fun `ensure fail when no concrete interface exists`() {

        val e = assertThatThrownBy {
            val capabilitiesScanner = CapabilitiesScanner(Clock())
            capabilitiesScanner.scan("cloud.orbit.runtime.special.capabilities.noconcrete")
        }

        e.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("Could not find mapping")
    }

    @Test
    fun `ensure fail when multiple concrete interfaces exist`() {

        val e = assertThatThrownBy {
            val capabilitiesScanner = CapabilitiesScanner(Clock())
            capabilitiesScanner.scan("cloud.orbit.runtime.special.capabilities.multiconcrete")
        }

        e.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("More than one concrete interface found")
    }

    @Test
    fun `ensure fail when multiple implementations of concrete interface exists`() {

        val e = assertThatThrownBy {
            val capabilitiesScanner = CapabilitiesScanner(Clock())
            capabilitiesScanner.scan("cloud.orbit.runtime.special.capabilities.multiimpl")
        }

        e.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("Multiple implementations of concrete interface")
    }

    @Test
    fun `ensure basic mapping is resolved`() {
        val capabilitiesScanner = CapabilitiesScanner(Clock())
        capabilitiesScanner.scan("cloud.orbit.runtime.special.capabilities.basic")

        assertThat(capabilitiesScanner.addressableClasses.contains(BasicCapabilitiesActor::class.java)).isTrue()

        assertThat(capabilitiesScanner.addressableInterfaces.contains(BasicCapabilities::class.java)).isTrue()

        assertThat(capabilitiesScanner.concreteAddressablesByClass[BasicCapabilitiesActor::class.java])
            .isEqualTo(BasicCapabilities::class.java)

        assertThat(capabilitiesScanner.concreteAddressablesByInterface[BasicCapabilities::class.java])
            .isEqualTo(BasicCapabilitiesActor::class.java)
    }

}