/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import com.nhaarman.mockitokotlin2.spy
import io.kotlintest.eventually
import io.kotlintest.matchers.doubles.shouldBeGreaterThan
import io.kotlintest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotlintest.milliseconds
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.ClusterManager
import orbit.server.service.Meters
import orbit.shared.mesh.NodeStatus
import org.junit.Test

class MetricsTests : BaseServerTest() {

    @Test
    fun `connecting to service increments connected metric`() {
        runBlocking {
            startServer()

            startClient()
            startClient()

            eventually(5.seconds) {
                Meters.ConnectedClients shouldBe 2.0
            }
        }
    }

    @Test
    fun `disconnecting from service decrements connected metric`() {
        runBlocking {
            startServer()
            val client = startClient()

            eventually(5.seconds) {
                Meters.ConnectedClients shouldBe 1.0
            }

            disconnectClient(client)
            eventually(5.seconds) {
                Meters.ConnectedClients shouldBe 0.0
            }
        }
    }

    @Test
    fun `sending messages to an addressable increments placements`() {
        runBlocking {
            startServer {
                instance(spy(resolve<ClusterManager>()) {
                    onBlocking {
                        this.getAllNodes()
                            .filter { node -> node.nodeStatus == NodeStatus.ACTIVE }
                    }.then {
                        advanceTime(100.milliseconds)
                        it.callRealMethod()
                    }
                })
            }
            val client = startClient()
            client.sendMessage("test message", "address 1")
            client.sendMessage("test message", "address 2")
            eventually(5.seconds) {
                Meters.PlacementTimer_Count shouldBe 2.0
                Meters.PlacementTimer_TotalTime shouldBeGreaterThan 0.0
            }
        }
    }

    @Test
    fun `sending a message to addressables increments total addressables`() {
        runBlocking {
            startServer()

            val client = startClient()
            client.sendMessage("test message", "address 1")
            client.sendMessage("test message", "address 2")
            eventually(5.seconds) {
                Meters.AddressableCount shouldBe 2.0
            }
        }
    }

    @Test
    fun `expiring an addressable decrements total addressables`() {
        runBlocking {
            val server = startServer(addressableLeaseDurationSeconds = 1)

            val client = startClient()
            client.sendMessage("test message", "address 1")
            server.tick()
            eventually(5.seconds) {
                Meters.AddressableCount shouldBe 1.0
            }

            advanceTime(5.seconds)
            server.tick()
            Meters.AddressableCount shouldBe 0.0
        }
    }

    @Test
    fun `adding nodes increments node count`() {
        runBlocking {
            startServer()

            eventually(5.seconds) {
                Meters.NodeCount shouldBe 1.0
            }

            startServer(port = 50057)

            eventually(5.seconds) {
                Meters.NodeCount shouldBe 2.0
            }
        }
    }

    @Test
    fun `expired node lease decrements node count`() {
        runBlocking {
            val server = startServer(nodeLeaseDurationSeconds = 60)

            val secondServer = startServer(port = 50057)
            secondServer.tick()
            Meters.NodeCount shouldBe 2.0

            disconnectServer(secondServer)
            server.tick()

            advanceTime(10.seconds)

            server.tick()
            Meters.NodeCount shouldBe 1.0
        }
    }

    @Test
    fun `connecting and disconnecting server nodes adjusts connected nodes count`() {
        runBlocking {
            val server = startServer()

            Meters.ConnectedNodes shouldBe 0.0

            val secondServer = startServer(port = 50057, tickRate = 100000.seconds)
            server.tick()
            secondServer.tick()

            Meters.ConnectedNodes shouldBe 1.0

            disconnectServer(secondServer)
            server.tick()
            Meters.ConnectedNodes shouldBe 0.0
        }
    }

    @Test
    fun `sending messages adds the payload size to the total`() {
        runBlocking {
            startServer()

            val client = startClient()
            client.sendMessage("test", "address 1")
            client.sendMessage("test 2", "address 1")
            eventually(5.seconds) {
                Meters.MessageSizes shouldBe 136.0
            }
        }
    }

    @Test
    fun `sending messages adds to the message count`() {
        runBlocking {
            startServer()

            val client = startClient()
            client.sendMessage("test", "address 1")
            client.sendMessage("test 2", "address 1")
            eventually(5.seconds) {
                Meters.MessagesCount shouldBe 2.0
            }
        }
    }

    @Test
    fun `constant tick timer going long increases Slow Tick count`() {
        runBlocking {
            var pulse = 0.seconds
            startServer(tickRate = 1.seconds) {
                instance(spy(resolve<ClusterManager>()) {
                    onBlocking { this.tick() }.then {
                        advanceTime(pulse)
                        pulse = 0.seconds
                        null
                    }
                })
            }
            pulse = 2.seconds

            eventually(10.seconds) {
                Meters.SlowTickCount shouldBeGreaterThan 0.0
            }
        }
    }

    @Test
    fun `constant tick timer elapses and records ticks`() {
        runBlocking {
            startServer(tickRate = 1.seconds) {
                instance(spy(resolve<ClusterManager>()) {
                    onBlocking { this.tick() }.then {
                        advanceTime(35.milliseconds)
                    }
                })
            }

            eventually(10.seconds) {
                Meters.TickTimer_Count shouldBeGreaterThan 3.0
                Meters.TickTimer_Total shouldBeGreaterThanOrEqual 0.0
            }

            println("Test over ${Meters.TickTimer_Total}")
        }
    }
}

