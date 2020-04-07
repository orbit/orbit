/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import orbit.client.actor.IdActor
import orbit.client.actor.createProxy
import orbit.server.service.Meters
import org.junit.Test
import kotlin.test.assertEquals

class LifecycleTests : BaseIntegrationTest() {
    @Test
    fun `Disconnecting a client removes it from the cluster`() {
        runBlocking {
            val key = "test"
            val result1 = client.actorFactory.createProxy<IdActor>(key).getId().await()
            assertEquals(result1, key)

            eventually(5.seconds) {
                Meters.ConnectedClients shouldBe 1.0
            }

            disconnectClient()

            eventually(5.seconds) {
                Meters.ConnectedClients shouldBe 0.0
            }
        }
    }
}