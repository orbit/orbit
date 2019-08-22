/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.application

import kotlinx.coroutines.runBlocking
import orbit.server.OrbitConfig
import orbit.server.OrbitServer

fun main() {
    val server1 = OrbitServer(OrbitConfig(grpcPort = 50056))
    val server2 = OrbitServer(OrbitConfig(grpcPort = 50057))

    runBlocking {
        server1.start().join()
        server2.start().join()
    }

    // TODO: Proper shutdown handling
    Thread.currentThread().join();
}