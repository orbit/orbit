/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.runBlocking
import orbit.client.actor.GreeterActor
import orbit.client.actor.createProxy
import org.junit.Test

class BasicTest : BaseIntegrationTest() {
    @Test
    fun basicStart() {
        val actor = client.actorFactory.createProxy<GreeterActor>()

        runBlocking {
            //actor.greetAsync("Joe").await()
        }
    }
}