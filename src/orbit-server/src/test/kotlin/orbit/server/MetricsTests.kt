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
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.rouz.grpc.ManyToManyCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
import org.junit.Test
import io.kotlintest.*

class MetricsTests {
    private val orbitServer: OrbitServer

    class MockMeterRegistry : SimpleMeterRegistry() {
        object Config : ExternallyConfigured<MeterRegistry> {
            override val instanceType = MockMeterRegistry::class.java
        }
    }

    init {
        orbitServer = OrbitServer(OrbitServerConfig(meterRegistry = MockMeterRegistry.Config))
        runBlocking {
            orbitServer.start()

            eventually(10.seconds) {
                orbitServer.nodeStatus shouldBe NodeStatus.ACTIVE
            }
        }
    }

    @Test
    fun `connecting to service increments connected metric`() {
        runBlocking {
            TestClient().connect()
            TestClient().connect()

            eventually(3.seconds) {
                Metrics.globalRegistry.meters[0].measure().first().value shouldBe 2.0
            }
        }
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

//    NodeInfo(
//    id = nodeId, capabilities = NodeCapabilities(), lease = NodeLease(
//    ChallengeToken(), Timestamp.now
//    (), Timestamp.now()
//    ), nodeStatus = NodeStatus.ACTIVE)

    private lateinit var connectionChannel: ManyToManyCall<Messages.MessageProto, Messages.MessageProto>

    suspend fun connect() {
        val response = NodeManagementGrpc.newStub(client).joinCluster(
            NodeManagementOuterClass.JoinClusterRequestProto.newBuilder().setCapabilities(
                Node.CapabilitiesProto.newBuilder().build()
            ).build()
        )
        nodeId = response.info.id.toNodeId()
        connectionChannel = ConnectionGrpc.newStub(client).openStream()
        println("joined cluster ${nodeId}")
    }

    suspend fun sendMessage(msg: String) {
        delay(300)
        val message =
            Message(MessageContent.InvocationRequest(AddressableReference("TestTarget", Key.NoKey), "report", msg))
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
