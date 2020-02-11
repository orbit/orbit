/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.runBlocking
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import orbit.server.mesh.LocalServerInfo
import org.junit.AfterClass
import org.junit.BeforeClass

open class BaseIntegrationTest {
    companion object {
        @JvmStatic
        protected lateinit var server: OrbitServer
            private set

        @JvmStatic
        protected lateinit var client: OrbitClient
            private set

        @JvmStatic
        protected val targetUri = "dns:///localhost:5874"

        @JvmStatic
        protected val namespace = "test"

        @BeforeClass
        @JvmStatic
        fun init() {
            server = OrbitServer(
                OrbitServerConfig(
                    LocalServerInfo(
                        port = 5874,
                        url = targetUri
                    )
                )
            )

            client = OrbitClient(
                OrbitClientConfig(
                    grpcEndpoint = targetUri,
                    namespace = namespace,
                    packages = listOf("orbit.client.actor")
                )
            )

            runBlocking {
                server.start().join()
                client.start().join()
            }
        }

        @AfterClass
        @JvmStatic
        fun deinit() {
            runBlocking {
                client.stop().join()
                server.stop().join()
            }
        }
    }

}