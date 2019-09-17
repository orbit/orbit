/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import kotlinx.coroutines.channels.ReceiveChannel
import orbit.common.di.ComponentProvider
import orbit.server.addressable.AddressableReference
import orbit.server.pipeline.Pipeline
import orbit.server.routing.MeshNode
import orbit.server.routing.Route
import orbit.shared.proto.Messages

internal class GrpcClient(
    override val id: NodeId = NodeId.generate("client:"),
    private val messages: ReceiveChannel<Messages.Message>,
    private val send: suspend (Messages.Message) -> Unit,
    private val container: ComponentProvider
) : MeshNode {

    suspend fun ready() {
        for (message in messages) {
            this.onNext(message)
        }
    }

    suspend override fun sendMessage(message: Message, route: Route?) {
        println("> ${this.id}: \"${message.content}\"")

        send(
            (when (message.content) {
                is MessageContent.ResponseErrorMessage ->
                    Messages.Message.newBuilder().setInvocationError(
                        Messages.InvocationErrorResponse.newBuilder().setMessage(message.content.toString())
                    )

                else -> Messages.Message.newBuilder().setInvocationResponse(
                    Messages.InvocationResponse.newBuilder().setValue(message.content.toString())
                )
            }).build()
        )
    }

    private fun onNext(value: Messages.Message) {
        when {
            value.hasInvocationRequest() -> {
                val msg = Message(
                    MessageContent.Request(
                        value.invocationRequest.value,
                        AddressableReference(
                            type = value.invocationRequest.reference.type,
                            id = value.invocationRequest.reference.id
                        )
                    ),
                    source = id,
                    target = MessageTarget.Unicast(NodeId("target"))
                )

                println("Client message ${msg}")
                val pipeline: Pipeline by container.inject()
                pipeline.pushInbound(msg)
            }
        }
    }
}
