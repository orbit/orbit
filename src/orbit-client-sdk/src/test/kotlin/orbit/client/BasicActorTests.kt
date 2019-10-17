/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.runBlocking
import orbit.client.actor.ActorWithNoImpl
import orbit.client.actor.GreeterActor
import orbit.client.actor.TimeoutActor
import orbit.client.actor.createProxy
import orbit.client.util.MessageException
import org.junit.Test

class BasicActorTests : BaseIntegrationTest() {
    @Test
    fun basicStart() {
        runBlocking {
            val actor = client.actorFactory.createProxy<GreeterActor>()
            //actor.greetAsync("Joe").await()
        }
    }

    @Test(expected = MessageException::class)
    fun `ensure invalid actor type throws`() {
        runBlocking {
            val actor = client.actorFactory.createProxy<ActorWithNoImpl>()
            actor.greetAsync("Joe").await()
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