/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

interface GreeterActor : ActorWithNoKey {
    fun greetAsync(name: String): Deferred<String>
}

class GreeterActorImpl : GreeterActor {
    override fun greetAsync(name: String): Deferred<String> =
        CompletableDeferred("Hello $name")
}