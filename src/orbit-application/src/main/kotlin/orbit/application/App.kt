/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.application

import kotlinx.coroutines.runBlocking
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig

fun main() {
    val server = OrbitServer(OrbitServerConfig())

    runBlocking {
        server.start().join()
    }
}