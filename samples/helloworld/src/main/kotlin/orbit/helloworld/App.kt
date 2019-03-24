/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld

import cloud.orbit.common.logging.getLogger
import cloud.orbit.common.logging.logger
import cloud.orbit.core.actor.AbstractActor
import cloud.orbit.core.actor.ActorWithNoKey
import cloud.orbit.core.actor.getReference
import cloud.orbit.runtime.stage.Stage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

interface Greeter : ActorWithNoKey {
    fun greet(name: String): Deferred<String>
}

@Suppress("UNUSED")
class GreeterActor : Greeter, AbstractActor() {
    private val logger by logger()

    override fun greet(name: String): Deferred<String> {
        logger.info("I was called by: $name. My identity is ${this.context.reference}")
        return CompletableDeferred("Hello $name!")
    }
}

fun main() {
    val logger = getLogger("main")
    val stage = Stage()

    runBlocking {
        stage.start().await()
        val greeter = stage.actorProxyFactory.getReference<Greeter>()
        val greeting = greeter.greet("Joe").await()
        logger.info("Response: $greeting")
        stage.stop().await()
    }
}
