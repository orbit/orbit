/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import orbit.client.actor.IdActor
import orbit.client.actor.createProxy
import orbit.client.net.ClientState
import org.junit.Test
import kotlin.test.assertEquals
import orbit.server.service.Meters.Companion as ServerMeters

class LifecycleTests : BaseIntegrationTest() {
    @Test
    fun `Running client is in Started state`() {
        runBlocking {
            eventually(5.seconds) {
                client.status shouldBe ClientState.CONNECTED
            }
        }

    }

    @Test
    fun `Stopping client goes into stopping state then idle`() {
        runBlocking {
            disconnectClient()

            eventually(5.seconds) {
                client.status shouldBe ClientState.STOPPING
            }

            eventually(5.seconds) {
                client.status shouldBe ClientState.IDLE
            }
        }
    }

    @Test
    fun `Connecting a client adds it to the server cluster`() {
        runBlocking {
            eventually(5.seconds) {
                ServerMeters.ConnectedClients shouldBe 1.0
            }
        }
    }

    @Test
    fun `Disconnecting a client removes it from the cluster`() {
        runBlocking {
            val key = "test"
            val result1 = client.actorFactory.createProxy<IdActor>(key).getId().await()
            assertEquals(result1, key)

            eventually(5.seconds) {
                ServerMeters.ConnectedClients shouldBe 1.0
            }

            disconnectClient()

            eventually(5.seconds) {
                ServerMeters.ConnectedClients shouldBe 0.0
            }
        }
    }

}