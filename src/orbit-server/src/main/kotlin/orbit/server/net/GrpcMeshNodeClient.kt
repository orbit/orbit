/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.rouz.grpc.ManyToManyCall
import orbit.server.routing.MeshNode
import orbit.server.routing.Route
import orbit.shared.proto.Addressable
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages
import orbit.shared.proto.messages

internal class GrpcMeshNodeClient(override val id: NodeId, private val channel: ManagedChannel) : MeshNode {
    private val sender: ManyToManyCall<Messages.Message, Messages.Message>

    init {

        fun notify(channel: ManagedChannel) {
            println("Channel state: ${id.value}: ${channel.getState(false)}")
            channel.notifyWhenStateChanged(channel.getState(true)) { notify(channel) }
        }

        notify(channel)
        sender = ConnectionGrpc.newStub(ClientInterceptors.intercept(channel, NodeIdClientInterceptor(id))).messages()
    }

    constructor(id: NodeId, host: String, port: Int) : this(
        id,
        ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()
    )

    override suspend fun sendMessage(message: Message, route: Route?) {

        val builder = Messages.Message.newBuilder()
        val toSend = when {
            message.content is MessageContent.Request ->
                builder.setInvocationRequest(
                    builder.invocationRequestBuilder
                        .setValue(message.content.data)
                        .setReference(
                            Addressable.AddressableReference.newBuilder()
                                .setId(message.content.destination.id)
                                .setType(message.content.destination.type).build()
                        )
                ).build()
            else -> null
        }

        if (toSend != null) {
            sender.send(toSend)
        }
    }
}
