/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import orbit.server.routing.MeshNode
import orbit.server.routing.Route
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages

internal class GrpcMeshNodeClient(override val id: NodeId, private val channel: ManagedChannel) : MeshNode,
    StreamObserver<Messages.Message> {

    private val sender: StreamObserver<Messages.Message>

    override fun onNext(value: Messages.Message?) {
        println("Message $id $value")
    }

    override fun onError(t: Throwable?) {
        println("Error $id $t")
    }

    override fun onCompleted() {
        println("Closed $id")
    }

    private val blockingStub: ConnectionGrpc.ConnectionStub

    init {
        fun notify(channel: ManagedChannel) {
            println("Channel state: ${id.value}: ${channel.getState(false)}")
            channel.notifyWhenStateChanged(channel.getState(true)) { notify(channel) }
        }

        notify(channel)
        blockingStub = ConnectionGrpc.newStub(ClientInterceptors.intercept(channel, NodeIdClientInterceptor(id)))
        sender = blockingStub.messages(this)
    }

    constructor(id: NodeId, host: String, port: Int) : this(
        id,
        ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()
    )

    override fun sendMessage(message: Message, route: Route?) {

        val builder = Messages.Message.newBuilder()
        val toSend = builder.setInvocationRequest(
            builder.invocationRequestBuilder.setValue(message.content.toString())
        ).build()

        sender.onNext(toSend)
    }
}
