/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.plugin.spring

import kotlinx.coroutines.runBlocking
import orbit.client.OrbitClient
import orbit.client.OrbitClientConfig
import orbit.client.actor.createProxy
import orbit.client.plugin.spring.actor.SpringTestActor
import orbit.client.plugin.spring.misc.SpringConfig
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import org.junit.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.test.assertTrue

class SpringAddressableConstructorTest {

    @Test
    fun `test spring actor injection`() {
        val ctx = AnnotationConfigApplicationContext()
        ctx.register(SpringConfig::class.java)
        ctx.refresh()

        val server = OrbitServer(OrbitServerConfig())
        val client = OrbitClient(OrbitClientConfig(
            addressableConstructor = SpringAddressableConstructor.Config(ctx),
            packages = listOf("orbit.client.plugin.spring.actor")
        ))

        runBlocking {
            server.start().join()
            client.start().join()

            val actor = client.actorFactory.createProxy<SpringTestActor>()
            val result1 = actor.getCallCount().await()
            val result2 = actor.getCallCount().await()
            assertTrue(result2 > result1)

            client.stop().join()
            server.stop().join()
        }
    }
}