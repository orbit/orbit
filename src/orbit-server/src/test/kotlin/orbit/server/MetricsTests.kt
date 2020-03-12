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
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.kotlintest.eventually
import io.kotlintest.matchers.doubles.shouldBeGreaterThan
import io.kotlintest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotlintest.milliseconds
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.rouz.grpc.ManyToManyCall
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LeaseDuration
import orbit.server.mesh.LocalServerInfo
import orbit.server.mesh.local.LocalAddressableDirectory
import orbit.server.mesh.local.LocalNodeDirectory
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeStatus
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Headers
import orbit.shared.proto.Messages
import orbit.shared.proto.Node
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.joinCluster
import orbit.shared.proto.openStream
import orbit.shared.proto.toMessageProto
import orbit.shared.proto.toNodeId
import orbit.util.di.ComponentContainerRoot
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import orbit.util.time.TimeMs
import org.junit.After
import org.junit.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class MetricsTests {
    private var clock: Clock = Clock()
    private var servers: MutableList<OrbitServer> = mutableListOf()

    class MockMeterRegistry : SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock()) {
        object Config : ExternallyConfigured<MeterRegistry> {
            override val instanceType = MockMeterRegistry::class.java
        }

        fun advanceTime(timeMs: TimeMs) {
            (this.clock as MockClock).add(timeMs, TimeUnit.MILLISECONDS)
        }
    }

    fun advanceTime(duration: Duration) {
        clock.advanceTime(duration.toMillis())
        Metrics.globalRegistry.registries.forEach { r -> (r as MockMeterRegistry)?.advanceTime(duration.toMillis()) }
    }

    @After
    fun afterTest() {
        runBlocking {
            servers.toList().forEach { server -> disconnectServer(server) }
            LocalNodeDirectory.clear()
            LocalAddressableDirectory.clear()
            Metrics.globalRegistry.clear()
            clock = Clock()
        }
    }

    private fun startServer(
        port: Int = 50056,
        addressableLeaseDurationSeconds: Long = 5,
        nodeLeaseDurationSeconds: Long = 10,
        containerOverrides: ComponentContainerRoot.() -> Unit = { }
    ): OrbitServer {
        val server = OrbitServer(
            OrbitServerConfig(
                serverInfo = LocalServerInfo(
                    port = port,
                    url = "localhost:${port}"
                ),
                meterRegistry = MockMeterRegistry.Config,
                addressableLeaseDuration = LeaseDuration(
                    Duration.ofSeconds(addressableLeaseDurationSeconds),
                    Duration.ofSeconds(addressableLeaseDurationSeconds)
                ),
                nodeLeaseDuration = LeaseDuration(
                    Duration.ofSeconds(nodeLeaseDurationSeconds),
                    Duration.ofSeconds(nodeLeaseDurationSeconds)
                ),
                clock = clock,
                containerOverrides = containerOverrides
            )
        )

        server.start()

        eventually(10.seconds) {
            server.nodeStatus shouldBe NodeStatus.ACTIVE
        }

        servers.add(server)
        return server
    }


    fun disconnectServer(server: OrbitServer?) {
        if (server == null) {
            return
        }

        server.stop()

        eventually(10.seconds) {
            server.nodeStatus shouldBe NodeStatus.STOPPED
        }

        servers.remove(server)
    }

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

class TestClient(port: Int = 50056) {
    private var nodeId: NodeId = NodeId.generate("test")
    private val client: Channel = ManagedChannelBuilder
        .forTarget("0.0.0.0:${port}")
        .usePlaintext()
        .intercept(TestAuthInterceptor { nodeId })
        .enableRetry()
        .build()

    private lateinit var connectionChannel: ManyToManyCall<Messages.MessageProto, Messages.MessageProto>

    suspend fun connect(): TestClient {
        val response = NodeManagementGrpc.newStub(client).joinCluster(
            NodeManagementOuterClass.JoinClusterRequestProto.newBuilder().setCapabilities(
                Node.CapabilitiesProto.newBuilder().addAddressableTypes("test").build()
            ).build()
        )
        nodeId = response.info.id.toNodeId()
        connectionChannel = ConnectionGrpc.newStub(client).openStream()
        return this
    }

    fun disconnect() {
        connectionChannel.close()
    }

    fun sendMessage(msg: String, address: String? = null) {
        val message = Message(
            MessageContent.InvocationRequest(
                AddressableReference(
                    "test",
                    if (address != null) Key.StringKey(address) else Key.NoKey
                ), "report",
                msg
            )
        )
        connectionChannel.send(message.toMessageProto())
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
