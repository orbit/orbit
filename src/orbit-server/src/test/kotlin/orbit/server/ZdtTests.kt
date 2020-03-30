/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ZdtTests : BaseServerTest() {

    @Test
    fun `when client leaves cluster, messages still are routed to client`() {
        runBlocking {
            var receivedMessages = object {
                var client1 = 0.0
                var client2 = 0.0
            }

            val server = startServer()
            val client1 = TestClient(scope = this, onReceive = { receivedMessages.client1++ }).connect()
            client1.sendMessage("test message 1", "address 1")
            eventually(5.seconds) {
                receivedMessages.client1.shouldBe(1.0)
            }

            val client2 = TestClient(scope = this, onReceive = { receivedMessages.client2++ }).connect()

            client1.drain()
            client2.sendMessage("test message 2", "address 1")

            eventually(5.seconds) {
                receivedMessages.client1.shouldBe(2.0)
            }

            client1.disconnect()
            client2.disconnect()
        }
    }

    @Test
    fun `when client leaves cluster, addressables are not placed on node`() {
        runBlocking {

        }
    }

}