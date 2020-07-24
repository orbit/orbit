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
import kotlinx.coroutines.delay
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
            startServer()

            val client = startClient()
            client.sendMessage("test message", "address 1")
            eventually(5.seconds) {
                Meters.AddressableCount shouldBe 1.0
            }

            advanceTime(5.seconds)
            eventually(5.seconds) {
                Meters.AddressableCount shouldBe 0.0
            }
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
            startServer()

            val secondServer = startServer(port = 50057)
            eventually(5.seconds) {
                Meters.NodeCount shouldBe 2.0
            }

            disconnectServer(secondServer)

            advanceTime(10.seconds)

            eventually(5.seconds) {
                Meters.NodeCount shouldBe 1.0
            }
        }
    }

    @Test
    fun `connecting and disconnecting server nodes adjusts connected nodes count`() {
        runBlocking {
            startServer()

            Meters.ConnectedNodes shouldBe 0.0
            val secondServer = startServer(port = 50057)

            eventually(5.seconds) {
                Meters.ConnectedNodes shouldBe 1.0
            }
            disconnectServer(secondServer)

            advanceTime(10.seconds)

            eventually(5.seconds) {
                Meters.NodeCount shouldBe 1.0
            }
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
            startServer {
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
            startServer() {
                instance(spy(resolve<ClusterManager>()) {
                    onBlocking { this.tick() }.then {
                        advanceTime(35.milliseconds)
                    }
                })
            }

            eventually(10.seconds) {
                Meters.TickTimer_Count shouldBeGreaterThan 3.0
//                Meters.TickTimer_Total shouldBeGreaterThanOrEqual 0.0
            }

            println("Test over ${Meters.TickTimer_Total}")
        }
    }
}

