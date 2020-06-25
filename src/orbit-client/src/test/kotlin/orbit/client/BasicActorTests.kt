/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import io.kotlintest.shouldBe
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import orbit.client.actor.ActorWithNoImpl
import orbit.client.actor.ArgumentOnDeactivate
import orbit.client.actor.BasicOnDeactivate
import orbit.client.actor.ComplexDtoActor
import orbit.client.actor.GreeterActor
import orbit.client.actor.IdActor
import orbit.client.actor.IncrementActor
import orbit.client.actor.NullActor
import orbit.client.actor.SuspendingMethodActor
import orbit.client.actor.TestException
import orbit.client.actor.ThrowingActor
import orbit.client.actor.TimeoutActor
import orbit.client.actor.TrackingGlobals
import orbit.client.actor.createProxy
import orbit.client.util.RemoteException
import orbit.client.util.TimeoutException
import orbit.util.misc.RNGUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicActorTests : BaseIntegrationTest() {
    @Test
    fun `test basic actor request response`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<GreeterActor>()
            val result = actor.greetAsync("Joe").await()
            assertEquals(result, "Hello Joe")
        }
    }

    @Test
    fun `test basic actor request response concurrent`() {
        runBlocking {
            val list = mutableListOf<Deferred<String>>()

            repeat(100) {
                val actor = client.actorFactory.createProxy<GreeterActor>()
                list += actor.greetAsync("Joe")
            }

            list.awaitAll().forEach {
                assertEquals(it, "Hello Joe")
            }
        }
    }

    @Test
    fun `test complex dto request response`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<ComplexDtoActor>()
            actor.complexCall(ComplexDtoActor.ComplexDto("Hello")).await()
        }
    }

    @Test(expected = RemoteException::class)
    fun `ensure throw fails`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<ThrowingActor>()
            actor.doThrow().await()
        }
    }

    @Test(expected = RemoteException::class)
    fun `ensure invalid actor type throws`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<ActorWithNoImpl>()
            actor.greetAsync("Daniel Jackson").await()
        }
    }

    @Test(expected = TimeoutException::class)
    fun `ensure message timeout throws`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<TimeoutActor>()
            actor.timeout().await()
        }
    }

    @Test
    fun `ensure actor reused`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<IncrementActor>()
            val result1 = actor.increment().await()
            val result2 = actor.increment().await()
            assertTrue(result2 > result1)
        }
    }

    @Test
    fun `ensure actor deactivates`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<IncrementActor>()
            val call1 = actor.increment().await()
            advanceTime(client.config.addressableTTL.multipliedBy(2))
            delay(client.config.tickRate.toMillis() * 2) // Wait twice the tick so the deactivation should have happened
            val call2 = actor.increment().await()
            assertTrue(call2 <= call1)
        }
    }

    @Test
    fun `ensure method onDeactivate`() {
        runBlocking {
            val before = TrackingGlobals.deactivateTestCounts.get()

            val argActor = client.actorFactory.createProxy<BasicOnDeactivate>()
            argActor.greetAsync("Test").await()

            val noArgActor = client.actorFactory.createProxy<ArgumentOnDeactivate>()
            noArgActor.greetAsync("Test").await()

            advanceTime(client.config.addressableTTL.multipliedBy(2))
            delay(client.config.tickRate.toMillis() * 2) // Wait twice the tick so the deactivation should have happened

            val after = TrackingGlobals.deactivateTestCounts.get()
            assertTrue(before < after)
        }
    }

    @Test
    fun `test actor with id and context`() {
        runBlocking {
            val actorKey = RNGUtils.randomString(128)
            val actor = client.actorFactory.createProxy<IdActor>(actorKey)
            val result = actor.getId().await()
            assertEquals(result, actorKey)
        }
    }

    @Test
    fun `test actor with simple null argument`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<NullActor>()
            val result = actor.simpleNull("Hi ", null).await()
            assertEquals("Hi null", result)
        }
    }

    @Test
    fun `test actor with complex null argument`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<NullActor>()
            val result = actor.complexNull("Bob ", null).await()
            assertEquals("Bob null", result)
        }
    }

    @Test(expected = TestException::class)
    fun `test platform exception`() {
        runBlocking {
            val customClient = startClient(
                namespace = "platformExceptionTest",
                platformExceptions = true
            )

            val actor = customClient.actorFactory.createProxy<ThrowingActor>()
            actor.doThrow().await()
        }
    }

    @Test
    fun `Calling an actor with a suspended method returns correct result`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<SuspendingMethodActor>("test")

            actor.ping("test message") shouldBe "test message"
        }
    }

    @Test
    fun `Deactivating actor with suspend onDeactivate calls deactivate`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<SuspendingMethodActor>("test")

            actor.ping("test message") shouldBe "test message"
            disconnectClient()

            TrackingGlobals.deactivateTestCounts.get() shouldBe 1
        }
    }
}
