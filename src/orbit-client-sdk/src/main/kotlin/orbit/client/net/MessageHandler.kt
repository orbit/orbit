/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import kotlinx.coroutines.CompletableDeferred
import mu.KotlinLogging
import orbit.client.OrbitClientConfig
import orbit.client.addressable.InvocationSystem
import orbit.client.util.MessageException
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.util.time.Clock
import orbit.util.time.TimeMs
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class MessageHandler(
    private val connectionHandler: ConnectionHandler,
    private val clock: Clock,
    private val invocationSystem: InvocationSystem,
    config: OrbitClientConfig
) {
    private data class ResponseEntry(
        val messageId: Long,
        val msg: Message,
        val completion: CompletableDeferred<Any>,
        val timeAdded: TimeMs
    )

    private val logger = KotlinLogging.logger { }
    private val messageCounter = AtomicLong(0)
    private val awaitingResponse = ConcurrentHashMap<Long, ResponseEntry>()
    private val messageTimeoutMs = config.messageTimeout.toMillis()

    fun onMessage(message: Message) {
        when (message.content) {
            is MessageContent.Error, is MessageContent.InvocationResponse -> {
                val messageId = message.messageId!!
                getCompletion(messageId)?.also { completion ->
                    when (val content = message.content) {
                        is MessageContent.Error -> {
                            completion.completeExceptionally(
                                MessageException("Exceptional response received: ${content.description}")
                            )
                        }
                    }
                }
            }
            is MessageContent.InvocationRequest -> {
                invocationSystem.onInvocationRequest(message)
            }
        }
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

    fun tick() {
        awaitingResponse.values.filter {
            it.timeAdded < clock.currentTime - messageTimeoutMs
        }.forEach {
            val content = "Response timed out after ${clock.currentTime - it.timeAdded}ms, timeout is" +
                    " ${messageTimeoutMs}ms. ${it.msg}"
            logger.warn(content)
            it.completion.completeExceptionally(
                MessageException(content)
            )
            awaitingResponse.remove(it.messageId)
        }
    }

    private fun getCompletion(messageId: Long): CompletableDeferred<Any>? {
        val msg = awaitingResponse.remove(messageId)
        if (msg == null) {
            logger.warn(
                "Response for unknown message $messageId received. Did it time out? " +
                        "(>${messageTimeoutMs}ms)."
            )
        }
        return msg?.completion
    }
}