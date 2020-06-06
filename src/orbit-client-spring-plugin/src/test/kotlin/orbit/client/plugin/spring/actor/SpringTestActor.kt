/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.plugin.spring.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import orbit.client.actor.ActorWithNoKey
import orbit.client.plugin.spring.misc.DemoSingleton
import org.springframework.beans.factory.annotation.Autowired

interface SpringTestActor : ActorWithNoKey {
    fun getCallCount(): Deferred<Int>
}

class SpringTestActorImpl(@Autowired private val demoSingleton: DemoSingleton) : SpringTestActor {
    override fun getCallCount(): Deferred<Int> =
        CompletableDeferred(demoSingleton.countUp())
}