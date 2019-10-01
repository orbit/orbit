/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import orbit.shared.mesh.NodeId
import orbit.shared.net.Message
import orbit.shared.proto.Messages
import orbit.shared.proto.toMessage
import orbit.shared.proto.toMessageProto

class ClientConnection(
    val nodeId: NodeId,
    private val incomingChannel: ReceiveChannel<Messages.MessageProto>,
    private val outgoingChannel: SendChannel<Messages.MessageProto>
) {
    suspend fun consumeMessages() {
        for (protoMessage in incomingChannel) {
            val message = protoMessage.toMessage()
            println(message.toString())
        }
    }

    suspend fun sendMessage(message: Message) {
        outgoingChannel.send(message.toMessageProto())
    }

    suspend fun close(cause: Throwable? = null) {
        outgoingChannel.close(cause)
    }
}