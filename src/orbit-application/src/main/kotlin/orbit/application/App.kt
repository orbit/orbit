/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.application

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import orbit.server.etcd.EtcdAddressableDirectory
import orbit.server.etcd.EtcdNodeDirectory
import java.time.Duration

fun main() {
    runBlocking {
        val server = OrbitServer(
            OrbitServerConfig(
                nodeDirectory = EtcdNodeDirectory.EtcdNodeDirectoryConfig(
                    System.getenv("NODE_DIRECTORY") ?: "0.0.0.0"
                ),

                addressableDirectory = EtcdAddressableDirectory.EtcdAddressableDirectoryConfig(
                    System.getenv("ADDRESSABLE_DIRECTORY") ?: "0.0.0.0"
                )
            )
        )
        delay(Duration.ofSeconds(5))
        server.start().join()
    }
}