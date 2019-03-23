/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld.dsl

import cloud.orbit.common.logging.getLogger
import cloud.orbit.core.actor.AbstractActor
import cloud.orbit.core.actor.getReference
import cloud.orbit.runtime.stage.Stage
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import orbit.helloworld.dsl.data.Greeting
import orbit.helloworld.dsl.data.Language
import java.util.concurrent.CompletableFuture

class GreeterActor : Greeter, AbstractActor() {
    override fun greet(name: String): CompletableFuture<Map<Language, Greeting>> {
        return CompletableFuture.completedFuture(
            mapOf(
                Language.ENGLISH to Greeting(Language.ENGLISH,"Hello $name!"),
                Language.GERMAN to Greeting(Language.GERMAN,"Hallo, $name!")
            )
        )
    }
}

fun main(args: Array<String>) {
    val logger = getLogger("app")
    val stage = Stage()

    runBlocking {
        stage.start().await()
        val greeter = stage.actorProxyFactory.getReference<Greeter>("test")
        greeter.greet("Cesar").await().forEach {
            logger.info("In ${it.key}: ${it.value.text}")
        }
        stage.stop().await()
    }
}
