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
import orbit.client.actor.KeyedDeactivatingActor
import orbit.client.actor.IdActor
import orbit.client.actor.SlowDeactivateActor
import orbit.client.actor.TrackingGlobals
import orbit.client.actor.createProxy
import orbit.client.net.ClientState
import orbit.server.service.Meters
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
    fun `Stopping client puts it into idle state`() {
        runBlocking {
            disconnectClient()
            client.status shouldBe ClientState.IDLE
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
            eventually(5.seconds) {
                ServerMeters.ConnectedClients shouldBe 1.0
            }

            disconnectClient()

            eventually(5.seconds) {
                ServerMeters.ConnectedClients shouldBe 0.0
            }
        }
    }

    @Test
    fun `Disconnecting a client sends actor to new client`() {
        runBlocking {
            val key = "test"
            val result1 = client.actorFactory.createProxy<IdActor>(key).getId().await()
            assertEquals(result1, key)
            disconnectClient()

            eventually(5.seconds) {
                ServerMeters.ConnectedClients shouldBe 0.0
            }

            // Assure addressable lease expires
            advanceTime(10.seconds)

            val result2 = startClient().actorFactory.createProxy<IdActor>(key).getId().await()

            result2 shouldBe key
        }
    }

    @Test
    fun `All actors get deactivated on shutdown`() {
        runBlocking {
            val count = 500
            repeat(count) { key ->
                client.actorFactory.createProxy<KeyedDeactivatingActor>(key).ping().await()
            }

            disconnectClient()

            eventually(5.seconds) {
                TrackingGlobals.deactivateTestCounts.get() shouldBe count
            }
        }
    }

    @Test
    fun `Concurrently deactivating actors doesn't exceed setting`() {
        runBlocking {
            repeat(100) { key ->
                client.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message").await()
            }

            disconnectClient()

            TrackingGlobals.maxConcurrentDeactivations.get() shouldBe 10
        }
    }
}
