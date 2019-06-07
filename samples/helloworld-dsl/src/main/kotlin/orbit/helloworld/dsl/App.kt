/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld.dsl

import cloud.orbit.common.logging.getLogger
import cloud.orbit.core.actor.AbstractActor
import cloud.orbit.core.actor.createProxy
import cloud.orbit.runtime.stage.Stage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import orbit.helloworld.dsl.data.kotlin.Greeting
import orbit.helloworld.dsl.data.kotlin.Language
import orbit.helloworld.dsl.kotlin.Greeter

class GreeterActor : Greeter, AbstractActor() {
    override fun greet(name: String): Deferred<Map<Language, Greeting>> =
        CompletableDeferred(
            mapOf(
                Language.ENGLISH to Greeting(Language.ENGLISH, "Hello $name!"),
                Language.GERMAN to Greeting(Language.GERMAN, "Hallo, $name!")
            )
        )
}

fun main() {
    val logger = getLogger("app")
    val stage = Stage()

    runBlocking {
        stage.start().await()
        val greeter = stage.actorProxyFactory.createProxy<Greeter>()
        greeter.greet("Cesar").await().forEach {
            logger.info("In ${it.key}: ${it.value.text}")
        }
        stage.stop().await()
    }
}
