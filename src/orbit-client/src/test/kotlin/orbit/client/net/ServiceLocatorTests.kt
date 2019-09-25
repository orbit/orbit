/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI

class ServiceLocatorTests {
    @Test
    fun `basic conversion`() {
        val locator = URI("orbit://localhost:1234/namespace").toServiceLocator()
        assertThat(locator.host).isEqualTo("localhost")
        assertThat(locator.port).isEqualTo(1234)
        assertThat(locator.namespace).isEqualTo("namespace")
    }

    @Test
    fun `must have orbit scheme`() {
        assertThrows<IllegalArgumentException> {
            URI("test://localhost:1234/test").toServiceLocator()
        }
    }

    @Test
    fun `must have port`() {
        assertThrows<IllegalArgumentException> {
            URI("orbit://localhost/test").toServiceLocator()
        }
    }

    @Test
    fun `must have namespace`() {
        assertThrows<IllegalArgumentException> {
            URI("orbit://localhost:1234").toServiceLocator()
        }

        assertThrows<IllegalArgumentException> {
            URI("orbit://localhost:1234/").toServiceLocator()
        }
    }
}