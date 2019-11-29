/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import orbit.client.actor.GreeterActor
import orbit.client.actor.createProxy
import orbit.client.net.OrbitServiceLocator
fun main() {
    val logger = KotlinLogging.logger {  }
    val targetUri = "orbit://localhost:50056/test"

    val client = OrbitClient(
        OrbitClientConfig(
            serviceLocator = OrbitServiceLocator(targetUri),
            packages = listOf("orbit.client.actor")
        )
    )

    runBlocking {
        client.start().join()
        val greeter = client.actorFactory.createProxy<GreeterActor>()
        do {
            try {
                val result = greeter.greetAsync("Joe").await()
                logger.info { result }
            }catch(e: Throwable) {
                logger.error { e }
            }
            delay(10000)
        } while(true)
    }
}