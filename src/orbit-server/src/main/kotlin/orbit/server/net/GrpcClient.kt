/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.server.*
import orbit.server.routing.*
import orbit.shared.proto.ConnectionOuterClass

internal class GrpcClient(
    override val id: NodeId = NodeId.generate(),
    private val responseObserver: StreamObserver<ConnectionOuterClass.MessageStreamResponse>,
    override val capabilities: List<Capability> = listOf(),
    private val onClientMessage: (Message) -> Unit = {}
) : MeshNode, StreamObserver<ConnectionOuterClass.Message> {

    override fun sendMessage(message: Message, route: Route?) {
        println("> ${this.id}: \"${message.content}\"")
        responseObserver.onNext(ConnectionOuterClass.MessageStreamResponse.newBuilder().setMessage(message.content.toString()).build())
    }

    override fun onError(t: Throwable?) {
        println("stream error")
        responseObserver.onError(t)
    }

    override fun onCompleted() {
        println("stream complete")
        responseObserver.onCompleted()
    }

    override fun onNext(value: ConnectionOuterClass.Message) {
        val msg = Message(
            MessageContent.Request(value.content, Address(AddressId(value.address))),
            target = MessageTarget.Unicast(NodeId("target"))
        )
        onClientMessage(msg)
    }

    override fun <T : Address> canHandle(address: T): Boolean {
        return true //this.capabilities.contains(address.capability())
    }
}
