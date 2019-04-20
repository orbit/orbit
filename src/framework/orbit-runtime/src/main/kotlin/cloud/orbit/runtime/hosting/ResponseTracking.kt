/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.exception.ResponseTimeoutException
import cloud.orbit.common.logging.logger
import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.TimeMs
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.stage.StageConfig
import java.util.concurrent.ConcurrentHashMap

internal class ResponseTracking(
    private val stageConfig: StageConfig,
    private val clock: Clock
) {
    private data class ResponseEntry(
        val messageId: Long,
        val msg: Message,
        val completion: Completion,
        val timeAdded: TimeMs
    )

    private val trackingMap: ConcurrentHashMap<Long, ResponseEntry> = ConcurrentHashMap()
    private val logger by logger()

    fun trackMessage(msg: Message, completion: Completion) {
        trackingMap.computeIfAbsent(msg.messageId!!) {
            ResponseEntry(
                messageId = msg.messageId,
                msg = msg,
                completion = completion,
                timeAdded = clock.currentTime
            )
        }
    }

    fun handleResponse(msg: Message) {
        when (msg.content) {
            is MessageContent.ResponseNormalMessage -> getCompletion(msg.messageId!!)?.complete(msg.content.response)
            is MessageContent.ResponseErrorMessage -> getCompletion(msg.messageId!!)?.completeExceptionally(msg.content.error)
            else -> throw NotImplementedError("Response tracking does not handle ${msg.content}")
        }
    }

    fun onTick() {
        trackingMap.values.filter {
            it.timeAdded < clock.currentTime - stageConfig.messageTimeoutMillis
        }.forEach {
            val content = "Response timed out after ${clock.currentTime - it.timeAdded}ms, timeout is" +
                    " ${stageConfig.messageTimeoutMillis}ms. ${it.msg}"
            logger.warn(content)
            it.completion.completeExceptionally(
                ResponseTimeoutException(content)
            )
            trackingMap.remove(it.messageId)
        }
    }

    private fun getCompletion(messageId: Long): Completion? {
        val msg = trackingMap.remove(messageId)
        if (msg == null) {
            logger.warn(
                "Response for message $messageId received after timeout " +
                        "(>${stageConfig.messageTimeoutMillis}ms)."
            )
        }
        return msg?.completion
    }
}