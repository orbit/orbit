/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import io.kotlintest.eventually
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.numerics.shouldBeLessThan
import io.kotlintest.matchers.numerics.shouldBeLessThanOrEqual
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import orbit.client.actor.IdActor
import orbit.client.actor.KeyedDeactivatingActor
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
            println("Starting test")
            repeat(100) { key ->
                client.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message").await()
            }
            println("Finished sending messages")

            delay(10)
            disconnectClient()

            println("Finished disconnecting client")
            println("Max deactivations: ${TrackingGlobals.maxConcurrentDeactivations.get()}")
            TrackingGlobals.maxConcurrentDeactivations.get() shouldBeLessThanOrEqual 10
        }
    }

    @Test
    fun `Actors added during deactivation get deactivated`() {
        runBlocking {
            var count = 100
            repeat(count) { key ->
                client.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message").await()
            }

            startServer(port = 50057, tickRate = 5.seconds)
            val client2 = startClient(port = 50057)

            var additionalAddressableCount = 0
            delay(500)
            GlobalScope.launch {
                repeat(100) { k ->
                    k.let { k + 100 }.let { key ->
                        if (client.status != ClientState.IDLE) {
                            client2.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message")
                            ++additionalAddressableCount
                            delay(10)
                        }
                    }
                }
            }

            disconnectClient()

            TrackingGlobals.deactivateTestCounts.get() shouldBeGreaterThan count
            TrackingGlobals.deactivateTestCounts.get() shouldBeLessThan count + additionalAddressableCount

            disconnectClient(client2)

            TrackingGlobals.deactivateTestCounts.get() shouldBe count + additionalAddressableCount
        }
    }
}
