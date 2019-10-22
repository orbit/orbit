/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import orbit.client.actor.ActorWithNoImpl
import orbit.client.actor.ComplexDtoActor
import orbit.client.actor.GreeterActor
import orbit.client.actor.IncrementActor
import orbit.client.actor.TimeoutActor
import orbit.client.actor.createProxy
import orbit.client.util.MessageException
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
    fun `test complex dto request response`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<ComplexDtoActor>()
            actor.complexCall(ComplexDtoActor.ComplexDto("Hello")).await()
        }
    }

    @Test(expected = MessageException::class)
    fun `ensure invalid actor type throws`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<ActorWithNoImpl>()
            actor.greetAsync("Daniel Jackson").await()
        }
    }

    @Test(expected = MessageException::class)
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
            client.clock.advanceTime(client.config.addressableTTL.toMillis() * 2)
            delay(client.config.tickRate.toMillis() * 2) // Wait twice the tick so the deactivation should have happened
            val call2 = actor.increment().await()
            assertTrue(call2 <= call1)
        }
    }


}