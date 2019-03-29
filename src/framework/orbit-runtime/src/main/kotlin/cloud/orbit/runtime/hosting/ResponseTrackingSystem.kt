/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.exception.ResponseTimeoutException
import cloud.orbit.common.logging.logger
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal class ResponseTrackingSystem(
    private val stageConfig: StageConfig,
    private val supervisorScope: SupervisorScope
) {
    private data class ResponseEntry(val msg: Message, val completion: Completion)

    private val trackingMap: ConcurrentHashMap<Long, ResponseEntry> = ConcurrentHashMap()
    private val logger by logger()

    fun trackMessage(msg: Message, completion: Completion) {
        trackingMap.computeIfAbsent(msg.messageId!!) {
            supervisorScope.launch {
                delay(stageConfig.messageTimeoutMillis)
                if (completion.isActive) {
                    val content = "Response timed out, took >${stageConfig.messageTimeoutMillis}ms. $msg"
                    logger.warn(content)
                    completion.completeExceptionally(
                        ResponseTimeoutException(content)
                    )
                }
            }
            ResponseEntry(
                msg = msg,
                completion = completion
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

    private fun getCompletion(messageId: Long): Completion? {
        val msg = trackingMap[messageId]
        if (msg == null) {
            logger.warn(
                "Response for message $messageId received after timeout " +
                        "(>${stageConfig.messageTimeoutMillis}ms)."
            )
        }
        return msg?.completion
    }
}