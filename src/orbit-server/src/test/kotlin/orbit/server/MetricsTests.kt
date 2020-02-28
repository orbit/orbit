/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

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
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.rouz.grpc.ManyToManyCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.LeaseDuration
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
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration

class MetricsTests {
    private var clock: Clock = Clock()
    private lateinit var orbitServer: OrbitServer

    class MockMeterRegistry : SimpleMeterRegistry() {
        object Config : ExternallyConfigured<MeterRegistry> {
            override val instanceType = MockMeterRegistry::class.java
        }
    }

    @Before
    fun connectServer() {
        orbitServer = OrbitServer(
            OrbitServerConfig(
                meterRegistry = MockMeterRegistry.Config,
                addressableLeaseDuration = LeaseDuration(Duration.ofSeconds(5), Duration.ofSeconds(5)),
                nodeLeaseDuration = LeaseDuration(Duration.ofSeconds(20), Duration.ofSeconds(20)),
                clock = clock
            )
        )

        runBlocking {
            orbitServer.start()

            eventually(10.seconds) {
                orbitServer.nodeStatus shouldBe NodeStatus.ACTIVE
            }
        }
    }

    @After
    fun disconnectServer() {
        runBlocking {
            orbitServer.stop()

            eventually(10.seconds) {
                orbitServer.nodeStatus shouldBe NodeStatus.STOPPED
            }

            Metrics.globalRegistry.clear()
            clock = Clock()
        }
    }

    @Test
    fun `connecting to service increments connected metric`() {
        runBlocking {
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
            val client = TestClient().connect()
            client.sendMessage("test message", "address 1")
            eventually(5.seconds) {
                AddressableCount shouldBe 1.0
            }

            clock.advanceTime(5000)
            eventually(5.seconds) {
                AddressableCount shouldBe 0.0
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

    }

}

class TestClient {
    private var nodeId: NodeId = NodeId.generate("test")
    private val client: Channel = ManagedChannelBuilder
        .forTarget("0.0.0.0:50056")
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
        println("joined cluster ${nodeId}")
        return this
    }

    suspend fun disconnect() {
        connectionChannel.close()
    }

    suspend fun sendMessage(msg: String, address: String? = null) {
        delay(300)
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
