/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import io.kotlintest.eventually
import io.kotlintest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotlintest.matchers.numerics.shouldBeLessThan
import io.kotlintest.matchers.numerics.shouldBeLessThanOrEqual
import io.kotlintest.milliseconds
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import orbit.client.actor.ClientAwareActor
import orbit.client.actor.KeyedDeactivatingActor
import orbit.client.actor.SlowDeactivateActor
import orbit.client.actor.TrackingGlobals
import orbit.client.actor.createProxy
import orbit.client.execution.AddressableDeactivator
import orbit.client.net.ClientState
import orbit.util.time.stopwatch
import org.junit.Test

class DeactivationTests : BaseIntegrationTest() {

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
    fun `Instant deactivation deactivates all addressables simultaneously`() {
        runBlocking {
            val client = startClient(
                addressableDeactivation = AddressableDeactivator.Instant.Config()
            )
            repeat(100) { key ->
                client.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message").await()
            }

            delay(10)
            disconnectClient(client)

            TrackingGlobals.maxConcurrentDeactivations.get() shouldBeGreaterThan 20
            TrackingGlobals.maxConcurrentDeactivations.get() shouldBeLessThan 100
        }
    }

    @Test
    fun `Concurrent deactivation doesn't exceed maximum concurrent setting`() {
        runBlocking {
            val client2 = startClient(
                addressableDeactivation = AddressableDeactivator.Concurrent.Config(10)
            )
            repeat(100) { key ->
                client2.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message").await()
            }

            delay(10)
            disconnectClient(client2)

            TrackingGlobals.maxConcurrentDeactivations.get() shouldBeLessThanOrEqual 10
        }
    }

    @Test
    fun `Actors added during deactivation are rejected`() {
        runBlocking {
            var count = 100
            repeat(count) { key ->
                client.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message").await()
            }

            startServer(port = 50057, tickRate = 5.seconds)
            val client2 = startClient(port = 50057, packages = listOf("orbit.client.alternativeActor"))

            GlobalScope.launch {
                repeat(100) { k ->
                    k.let { k + 100 }.let { key ->
                        if (client.status != ClientState.IDLE) {
                            delay(5)
                            client2.actorFactory.createProxy<SlowDeactivateActor>(key).ping("message")
                        }
                    }
                }
            }

            disconnectClient(client, AddressableDeactivator.TimeSpan(AddressableDeactivator.TimeSpan.Config(500)))

            TrackingGlobals.deactivateTestCounts.get() shouldBe count
        }
    }

    @Test
    fun `Deactivating by rate limit takes minimum time with addressable count`() {
        runBlocking {
            // Only use custom client
            disconnectClient()
            val count = 500
            val client = startClient(
                addressableDeactivation = AddressableDeactivator.RateLimited.Config(1000)
            )

            repeat(count) { key ->
                client.actorFactory.createProxy<KeyedDeactivatingActor>(key).ping().await()
            }

            val watch = stopwatch(clock) {
                disconnectClient(client)
            }

            watch.elapsed shouldBeGreaterThanOrEqual 500
            watch.elapsed shouldBeLessThan 700
        }
    }

    @Test
    fun `Deactivating by timespan takes minimum timespan regardless of addressable count`() {
        runBlocking {
            // Only use custom client
            disconnectClient()

            var key = 0

            suspend fun test(count: Int, deactivationTime: Long) {
                println("Testing ${count} deactivations in ${deactivationTime}ms")
                val client = startClient(
                    addressableDeactivation = AddressableDeactivator.TimeSpan.Config(deactivationTime)
                )

                repeat(count) {
                    client.actorFactory.createProxy<KeyedDeactivatingActor>(key++).ping().await()
                }

                val watch = stopwatch(clock) {
                    disconnectClient(client)
                }

                println("Deactivated in ${watch.elapsed}ms")
                watch.elapsed.toDouble() shouldBeGreaterThanOrEqual deactivationTime * .95
                watch.elapsed shouldBeLessThan deactivationTime * 4
            }

            test(100, 500)
            test(500, 500)
            test(2000, 500)
        }
    }

    @Test
    fun `Specifying a shutdown mechanism during stop overrides configured shutdown`() {
        runBlocking {
            val deactivationTime = 500L

            repeat(500) { key ->
                client.actorFactory.createProxy<SlowDeactivateActor>(key).ping().await()
            }

            val watch = stopwatch(clock) {
                disconnectClient(
                    client, AddressableDeactivator.TimeSpan(
                        AddressableDeactivator.TimeSpan.Config(
                            deactivationTime
                        )
                    )
                )
            }

            watch.elapsed shouldBeGreaterThanOrEqual deactivationTime
            watch.elapsed shouldBeLessThan deactivationTime + 200
        }
    }

    // start shutdown rate
    // wait first time
    // send message to first
    // receive message on client 2?

    @Test
    fun `Sending a message to a deactivated actor during shutdown reroutes the message`() {
        runBlocking {
            // Only use custom client
            disconnectClient()

            val client = startClient(addressableTTL = 300.milliseconds)

            val clientId = client.nodeId

            repeat(5) { key ->
                client.actorFactory.createProxy<ClientAwareActor>(key.toString()).getClient().await() shouldBe clientId
            }

            val client2 = startClient()
            val client2Id = client2.nodeId

            val job = client.stop(AddressableDeactivator.RateLimited(AddressableDeactivator.RateLimited.Config(10)))

            delay(150)

            val deactivated = TrackingGlobals.deactivatedActors.first()
            val actor = client2.actorFactory.createProxy<ClientAwareActor>(deactivated.toString())

            actor.getClient().await() shouldBe client2Id

            job.join()
        }
    }


}