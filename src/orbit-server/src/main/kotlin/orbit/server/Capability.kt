/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

data class Capability(val type: String) {
    companion object Named {

        @JvmStatic
        val Mesh: Capability = Capability("Mesh")

        @JvmStatic
        val Routing: Capability = Capability("Routing")

        @JvmStatic
        val None: Capability = Capability("")
    }
}
