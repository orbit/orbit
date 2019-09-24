/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.runBlocking
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class BaseIntegrationTest {
    private val targetUri = URI("orbit://localhost:5874/test")

    lateinit var server: OrbitServer
        private set

    lateinit var client: OrbitClient
        private set

    @BeforeAll
    fun init() {
        server = OrbitServer(OrbitServerConfig(grpcPort = targetUri.port))
        client = OrbitClient(OrbitClientConfig(serviceURI = targetUri))

        runBlocking {
            server.start().join()
            client.start().join()
        }
    }

    @AfterAll
    fun deinit() {
        runBlocking {
            client.stop().join()
            server.stop().join()
        }
    }
}