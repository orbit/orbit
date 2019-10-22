/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import orbit.server.auth.AuthInfo
import orbit.server.pipeline.Pipeline
import orbit.shared.exception.CapacityExceededException
import orbit.shared.exception.toErrorContent
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget
import orbit.shared.proto.Messages
import orbit.shared.proto.toMessage
import orbit.shared.proto.toMessageProto
import orbit.shared.router.Route

class ClientConnection(
    private val authInfo: AuthInfo,
    private val incomingChannel: ReceiveChannel<Messages.MessageProto>,
    private val outgoingChannel: SendChannel<Messages.MessageProto>,
    private val pipeline: Pipeline
) : MessageSender {
    suspend fun consumeMessages() {
        for (protoMessage in incomingChannel) {

            val message = protoMessage.toMessage()
            val meta = MessageMetadata(
                authInfo = authInfo,
                messageDirection = MessageDirection.INBOUND
            )

            try {
                pipeline.pushMessage(message, meta)

            } catch (t: Throwable) {
                pipeline.pushMessage(
                    msg = Message(
                        target = message.source?.let(MessageTarget::Unicast),
                        messageId = message.messageId,
                        content = t.toErrorContent()
                    ),
                    meta = MessageMetadata(
                        authInfo = authInfo,
                        messageDirection = MessageDirection.OUTBOUND,
                        respondOnError = false
                    )
                )
            }
        }
    }

    override suspend fun sendMessage(message: Message, route: Route?) {
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