/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import orbit.server.pipeline.Pipeline
import orbit.shared.exception.CapacityExceededException
import orbit.shared.exception.toErrorContent
import orbit.shared.mesh.NodeId
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget
import orbit.shared.proto.Messages
import orbit.shared.proto.toMessage
import orbit.shared.proto.toMessageProto

class ClientConnection(
    private val nodeId: NodeId,
    private val incomingChannel: ReceiveChannel<Messages.MessageProto>,
    private val outgoingChannel: SendChannel<Messages.MessageProto>,
    private val pipeline: Pipeline
) {
    suspend fun consumeMessages() {
        for (protoMessage in incomingChannel) {

            val message = protoMessage.toMessage().copy(
                // We don't trust the client to specify its own ID
                source = nodeId
            )

            try {
                pipeline.pushInbound(message)

            } catch (t: Throwable) {
                pipeline.pushOutbound(
                    Message(
                        target = message.source?.let(MessageTarget::Unicast),
                        messageId = message.messageId,
                        content = t.toErrorContent()
                    )
                )
            }
        }
    }

    suspend fun sendMessage(message: Message) {
        outgoingChannel.send(message.toMessageProto())
    }

    fun offerMessage(messsage: Message) {
        val queued = outgoingChannel.offer(messsage.toMessageProto())
        if (!queued) throw CapacityExceededException("Could not offer message.")
    }

    fun close(cause: Throwable? = null, messageId: Long? = null) {
        offerMessage(
            Message(
                messageId = messageId,
                content = cause.toErrorContent()
            )
        )
        outgoingChannel.close()
    }


}