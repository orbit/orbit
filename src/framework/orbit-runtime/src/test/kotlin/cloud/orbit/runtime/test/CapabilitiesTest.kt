/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.common.time.Clock
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CapabilitiesTest {
    @Test
    fun `ensure fail when no concrete interface exists`() {
        assertThrows<IllegalStateException> {
            val capabilitiesScanner = CapabilitiesScanner(Clock())
            capabilitiesScanner.scan("cloud.orbit.runtime.special.noconcrete")
        }
    }
}