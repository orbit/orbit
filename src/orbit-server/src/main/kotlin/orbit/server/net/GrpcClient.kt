/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.server.Address
import orbit.server.AddressId
import orbit.server.Capability
import orbit.server.routing.MeshNode
import orbit.server.routing.Route
import orbit.shared.proto.Messages

internal class GrpcClient(
    override val id: NodeId = NodeId.generate("client"),
    private val responseObserver: StreamObserver<Messages.Message>,
    override val capabilities: List<Capability> = listOf(),
    private val onClientMessage: (Message) -> Unit = {}
) : MeshNode, StreamObserver<Messages.Message> {

    override fun sendMessage(message: Message, route: Route?) {
        println("> ${this.id}: \"${message.content}\"")
        Messages.Message.newBuilder().setInvocationResponse(
            Messages.InvocationResponse .newBuilder().setValue(message.content.toString())
        )
            .build()
            .also {
                responseObserver.onNext(it)
            }
    }

    override fun onError(t: Throwable?) {
        println("stream error")
        responseObserver.onError(t)
    }

    override fun onCompleted() {
        println("stream complete")
        responseObserver.onCompleted()
    }

    override fun onNext(value: Messages.Message) {
        when {
            value.hasInvocationRequest() -> {
                val msg = Message(
                    MessageContent.Request(value.invocationRequest.value, Address(AddressId(value.invocationRequest.reference.id))),
                    target = MessageTarget.Unicast(NodeId("target"))
                )
                onClientMessage(msg)
            }
        }

    }

    override fun <T : Address> canHandle(address: T): Boolean {
        return true //this.capabilities.contains(address.capability())
    }
}
