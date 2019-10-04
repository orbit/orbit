/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.runBlocking
import orbit.client.net.OrbitServiceLocator
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import orbit.shared.net.PortBinding
import org.junit.AfterClass
import org.junit.BeforeClass
import java.net.URI

open class BaseIntegrationTest {
    companion object {
        protected lateinit var server: OrbitServer
            private set

        protected lateinit var client: OrbitClient
            private set

        private val targetUri = URI("orbit://localhost:5874/test")

        @BeforeClass
        @JvmStatic
        fun init() {
            server = OrbitServer(OrbitServerConfig(serverPort = PortBinding(targetUri.host, targetUri.port)))
            client = OrbitClient(OrbitClientConfig(serviceLocator = OrbitServiceLocator(targetUri)))

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