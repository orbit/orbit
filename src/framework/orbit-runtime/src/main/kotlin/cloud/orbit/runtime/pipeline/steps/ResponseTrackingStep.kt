/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.runtime.hosting.ResponseTrackingSystem
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.net.MessageTarget
import cloud.orbit.runtime.pipeline.PipelineContext
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletionException

class ResponseTrackingStep(private val responseTracking: ResponseTrackingSystem) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        when (msg.content) {
            is MessageContent.RequestInvocationMessage -> responseTracking.trackMessage(msg, context.completion!!)
            else -> Unit // Do nothing
        }
        context.nextOutbound(msg)
    }

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when (msg.content) {
            is MessageContent.ResponseErrorMessage,
            is MessageContent.ResponseNormalMessage ->
                responseTracking.handleResponse(msg)

            is MessageContent.RequestInvocationMessage -> {
                // TODO: Don't convert to CF
                context.completion!!.asCompletableFuture().handle { res, err ->
                    val newContent = if (err != null && err !is CompletionException) {
                        MessageContent.ResponseErrorMessage(err)
                    } else {
                        MessageContent.ResponseNormalMessage(res)
                    }

                    val newMsg = Message(
                        messageId = msg.messageId,
                        target = MessageTarget.Unicast(msg.source!!),
                        content = newContent
                    )

                    context.newOutbound(newMsg)
                }
                context.nextInbound(msg)
            }
        }
    }
}