/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.application

import kotlinx.coroutines.runBlocking
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import orbit.server.etcd.EtcdAddressableDirectory
import orbit.server.etcd.EtcdNodeDirectory

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
        server.start().join()
    }
}