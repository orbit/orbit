/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.grpc.ManagedChannelBuilder
import io.rouz.grpc.ManyToManyCall
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.mesh.NodeId
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.proto.AddressableManagementGrpc
import orbit.shared.proto.AddressableManagementOuterClass
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages
import orbit.shared.proto.Node
import orbit.shared.proto.NodeManagementGrpc
import orbit.shared.proto.NodeManagementOuterClass
import orbit.shared.proto.joinCluster
import orbit.shared.proto.leaveCluster
import orbit.shared.proto.openStream
import orbit.shared.proto.renewLease
import orbit.shared.proto.toAddressableLease
import orbit.shared.proto.toAddressableReferenceProto
import orbit.shared.proto.toMessage
import orbit.shared.proto.toMessageProto
import orbit.shared.proto.toNodeId

class TestClient(private val onReceive: (msg: Message) -> Unit = {}) {
    var nodeId: NodeId = NodeId.generate("test")

    private lateinit var connectionChannel: ManyToManyCall<Messages.MessageProto, Messages.MessageProto>
    private lateinit var nodeChannel: NodeManagementGrpc.NodeManagementStub
    private lateinit var addressableChannel: AddressableManagementGrpc.AddressableManagementStub

    private var messageId = 0L

    suspend fun connect(port: Int = 50056): TestClient {
        val channel = ManagedChannelBuilder.forTarget("0.0.0.0:${port}")
            .usePlaintext()
            .intercept(TestAuthInterceptor { nodeId })
            .enableRetry()
            .build()

        nodeChannel = NodeManagementGrpc.newStub(channel)

        val response = nodeChannel.joinCluster(
            NodeManagementOuterClass.JoinClusterRequestProto.newBuilder().setCapabilities(
                Node.CapabilitiesProto.newBuilder().addAddressableTypes("test").build()
            ).build()
        )
        nodeId = response.info.id.toNodeId()
        connectionChannel = ConnectionGrpc.newStub(channel).openStream()
        addressableChannel = AddressableManagementGrpc.newStub(channel)

        GlobalScope.launch {
            for (msg in connectionChannel) {
                onMessage(msg.toMessage())
            }
        }

        return this
    }

    fun disconnect() {
        connectionChannel.close()
    }

    suspend fun drain() {
        nodeChannel.leaveCluster(
            NodeManagementOuterClass.LeaveClusterRequestProto.newBuilder().build()
        )
    }

    fun onMessage(msg: Message) {
        val content = if (msg.content is MessageContent.InvocationRequest)
            (msg.content as MessageContent.InvocationRequest).arguments
        else
            "Error: ${(msg.content as MessageContent.Error).description}"
        println("Message received on node ${nodeId} - ${content}")
        onReceive(msg)
    }

    fun sendMessage(msg: String, address: String? = null) {
        println("Sending message to ${address} - ${msg}")
        val message = Message(
            MessageContent.InvocationRequest(
                AddressableReference(
                    "test",
                    if (address != null) Key.StringKey(address) else Key.NoKey
                ), "report",
                msg
            ),
            source = nodeId,
            messageId = ++messageId
        )
        connectionChannel.send(message.toMessageProto())
    }

    suspend fun renewAddressableLease(address: String) : AddressableLease? {
        val response = addressableChannel.renewLease(
            AddressableManagementOuterClass.RenewAddressableLeaseRequestProto.newBuilder()
                .setReference(AddressableReference("test", Key.of(address)).toAddressableReferenceProto())
                .build()
        )

        return response.lease?.toAddressableLease()
    }
}