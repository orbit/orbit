/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import com.nhaarman.mockitokotlin2.spy
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.kotlintest.eventually
import io.kotlintest.matchers.doubles.shouldBeGreaterThan
import io.kotlintest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotlintest.milliseconds
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.ClusterManager
import orbit.shared.mesh.NodeId
import orbit.shared.proto.Headers
import org.junit.Test

class MetricsTests : BaseServerTest() {

    @Test
    fun `connecting to service increments connected metric`() {
        runBlocking {
            startServer()

            TestClient().connect()
            TestClient().connect()

            eventually(5.seconds) {
                ConnectedClients shouldBe 2.0
            }
        }
    }

    @Test
    fun `disconnecting from service decrements connected metric`() {
        runBlocking {
            startServer()
            val client = TestClient().connect()

            eventually(5.seconds) {
                ConnectedClients shouldBe 1.0
            }

            client.disconnect()
            eventually(5.seconds) {
                ConnectedClients shouldBe 0.0
            }
        }
    }

    @Test
    fun `sending messages to an addressable increments placements`() {
        runBlocking {
            startServer {
                instance(spy(resolve<ClusterManager>()) {
                    onBlocking { this.getAllNodes() }.then {
                        advanceTime(100.milliseconds)
                        it.callRealMethod()
                    }
                })
            }
            val client = TestClient().connect()
            client.sendMessage("test message", "address 1")
            client.sendMessage("test message", "address 2")
            eventually(5.seconds) {
                PlacementTimer_Count shouldBe 2.0
                PlacementTimer_TotalTime shouldBeGreaterThan 0.0
            }
        }
    }

    @Test
    fun `sending a message to addressables increments total addressables`() {
        runBlocking {
            startServer()

            val client = TestClient().connect()
            client.sendMessage("test message", "address 1")
            client.sendMessage("test message", "address 2")
            eventually(5.seconds) {
                AddressableCount shouldBe 2.0
            }
        }
    }

    @Test
    fun `expiring an addressable decrements total addressables`() {
        runBlocking {
            startServer()

            val client = TestClient().connect()
            client.sendMessage("test message", "address 1")
            eventually(5.seconds) {
                AddressableCount shouldBe 1.0
            }

            advanceTime(5.seconds)
            eventually(5.seconds) {
                AddressableCount shouldBe 0.0
            }
        }
    }

    @Test
    fun `adding nodes increments node count`() {
        runBlocking {
            startServer()

            eventually(5.seconds) {
                NodeCount shouldBe 1.0
            }

            startServer(port = 50057)

            eventually(5.seconds) {
                NodeCount shouldBe 2.0
            }
        }
    }

    @Test
    fun `expired node lease decrements node count`() {
        runBlocking {
            startServer()

            val secondServer = startServer(port = 50057)
            eventually(5.seconds) {
                NodeCount shouldBe 2.0
            }

            disconnectServer(secondServer)

            advanceTime(10.seconds)

            eventually(5.seconds) {
                NodeCount shouldBe 1.0
            }
        }
    }

    @Test
    fun `connecting node to server increments connected nodes count`() {
        runBlocking {
            startServer()

            ConnectedNodes shouldBe 0.0
            val secondServer = startServer(port = 50057)

            eventually(5.seconds) {
                ConnectedNodes shouldBe 1.0
            }

            disconnectServer(secondServer)

            advanceTime(10.seconds)

            eventually(5.seconds) {
                NodeCount shouldBe 1.0
            }
        }
    }

    @Test
    fun `disconnecting node from server decrements connected nodes count`() {
        runBlocking {
            startServer()

            ConnectedNodes shouldBe 0.0
            val secondServer = startServer(port = 50057)

            eventually(5.seconds) {
                ConnectedNodes shouldBe 1.0
            }

            disconnectServer(secondServer)

            advanceTime(10.seconds)

            eventually(5.seconds) {
                NodeCount shouldBe 1.0
            }
        }
    }

    @Test
    fun `sending messages adds the payload size to the total`() {
        runBlocking {
            startServer()

            val client = TestClient().connect()
            client.sendMessage("test", "address 1")
            client.sendMessage("test 2", "address 1")
            eventually(5.seconds) {
                MessageSizes shouldBe 80.0
            }
        }
    }

    @Test
    fun `sending messages adds to the message count`() {
        runBlocking {
            startServer()

            val client = TestClient().connect()
            client.sendMessage("test", "address 1")
            client.sendMessage("test 2", "address 1")
            eventually(5.seconds) {
                MessagesCount shouldBe 2.0
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

            eventually(5.seconds) {
                SlowTickCount shouldBeGreaterThan 0.0
            }
        }
    }

    @Test
    fun `constant tick timer elapses`() {
        runBlocking {
            startServer {
                instance(spy(resolve<ClusterManager>()) {
                    onBlocking { this.tick() }.then {
                        advanceTime(500.milliseconds)
                    }
                })
            }

            eventually(2.seconds) {
                TickTimer_Count shouldBeGreaterThan 1.0
                TickTimer_Total shouldBeGreaterThanOrEqual .5
            }
        }
    }

    companion object {
        private fun getMeter(name: String, statistic: String? = null): Double {
            return Metrics.globalRegistry.meters.first { m -> m.id.name == name }.measure()
                .first { m -> statistic == null || statistic.equals(m.statistic.name, true) }.value
        }

        private val ConnectedClients: Double get() = getMeter("Connected Clients")
        private val PlacementTimer_Count: Double get() = getMeter("Placement Timer", "count")
        private val PlacementTimer_TotalTime: Double get() = getMeter("Placement Timer", "total_time")
        private val AddressableCount: Double get() = getMeter("Addressable Count")
        private val NodeCount: Double get() = getMeter("Node Count")
        private val ConnectedNodes: Double get() = getMeter("Connected Nodes")
        private val MessagesCount: Double get() = getMeter("Message Sizes", "count")
        private val MessageSizes: Double get() = getMeter("Message Sizes", "total")
        private val SlowTickCount: Double get() = getMeter("Slow Ticks")
        private val TickTimer_Count: Double get() = getMeter("Tick Timer", "count")
        private val TickTimer_Total: Double get() = getMeter("Tick Timer", "total_time")
    }
}

internal class TestAuthInterceptor(private val getNodeId: () -> NodeId) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object :
            ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>?, headers: Metadata) {
                val nodeId = getNodeId()
                headers.put(NAMESPACE, nodeId.namespace)
                headers.put(NODE_KEY, nodeId.key)

                super.start(responseListener, headers)
            }
        }
    }

    companion object {
        private val NAMESPACE = Metadata.Key.of(Headers.NAMESPACE_NAME, Metadata.ASCII_STRING_MARSHALLER)
        private val NODE_KEY = Metadata.Key.of(Headers.NODE_KEY_NAME, Metadata.ASCII_STRING_MARSHALLER)
    }
}
