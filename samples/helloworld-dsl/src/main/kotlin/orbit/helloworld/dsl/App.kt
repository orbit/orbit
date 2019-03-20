/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld.dsl

import cloud.orbit.common.logging.getLogger
import cloud.orbit.core.actor.AbstractActor
import cloud.orbit.core.actor.ActorWithStringKey
import cloud.orbit.core.actor.getReference
import cloud.orbit.runtime.stage.Stage
import cloud.orbit.runtime.stage.StageConfig
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

class GreeterActor : Greeter, AbstractActor() {
    override fun greet(name: String): CompletableFuture<Greeting> {
        return CompletableFuture.completedFuture(Greeting("Hello $name!"))
    }
}

fun main(args: Array<String>) {
    val logger = getLogger("app")
    val stage = Stage()

    runBlocking {
        stage.start().await()
        val greeter = stage.actorProxyFactory.getReference<Greeter>("test")
        logger.info(greeter.greet("Cesar").await().greeting)
        stage.stop().await()
    }
}
