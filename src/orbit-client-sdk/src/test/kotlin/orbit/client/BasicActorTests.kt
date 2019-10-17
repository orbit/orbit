/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.runBlocking
import orbit.client.actor.ActorWithNoImpl
import orbit.client.actor.ComplexDtoActor
import orbit.client.actor.GreeterActor
import orbit.client.actor.TimeoutActor
import orbit.client.actor.createProxy
import orbit.client.util.MessageException
import org.junit.Test

class BasicActorTests : BaseIntegrationTest() {
    @Test
    fun `test basic actor request response`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<GreeterActor>()
            //actor.greetAsync("Joe").await()
        }
    }

    @Test
    fun `test complex dto request response`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<ComplexDtoActor>()
            //actor.complexCall(ComplexDtoActor.ComplexDto("Hello")).await()
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
}