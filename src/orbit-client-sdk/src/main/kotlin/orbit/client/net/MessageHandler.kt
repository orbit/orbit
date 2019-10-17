/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import kotlinx.coroutines.CompletableDeferred
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.util.time.Clock
import orbit.util.time.TimeMs
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class MessageHandler(
    private val connectionHandler: ConnectionHandler,
    private val clock: Clock
) {
    private data class ResponseEntry(
        val messageId: Long,
        val msg: Message,
        val completion: CompletableDeferred<Any>,
        val timeAdded: TimeMs
    )

    private val messageCounter = AtomicLong(0)
    private val awaitingResponse = ConcurrentHashMap<Long, ResponseEntry>()

    suspend fun onMessage(message: Message) {
        println(message)
    }

    fun sendMessage(msg: Message): CompletableDeferred<Any> {
        val messageId = msg.messageId ?: messageCounter.incrementAndGet()
        val newMsg = msg.copy(
            messageId = messageId
        )
        val entry = ResponseEntry(
            messageId = messageId,
            msg = newMsg,
            completion = CompletableDeferred(),
            timeAdded = clock.currentTime
        )

        if (newMsg.content is MessageContent.InvocationRequest) {
            awaitingResponse[messageId] = entry
        } else {
            entry.completion.complete(Unit)
        }

        connectionHandler.send(newMsg)

        return entry.completion
    }
}