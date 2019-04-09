/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableInvocationType
import cloud.orbit.runtime.hosting.ResponseTrackingSystem
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.pipeline.PipelineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal class ResponseTrackingStep(private val responseTracking: ResponseTrackingSystem) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        when (msg.content) {
            is MessageContent.RequestInvocationMessage -> {
                when (msg.content.addressableInvocation.invocationType) {
                    AddressableInvocationType.REQUEST_RESPONSE -> {
                        // We start tracking this for a response later
                        responseTracking.trackMessage(msg, context.completion)
                    }
                    AddressableInvocationType.ONE_WAY -> {
                        // We immediately complete and do not track
                        context.completion.complete(Unit)
                        // Further errors will be treated as unhandled.
                        context.suppressErrors = false
                    }
                }
            }
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
                // We only have work to do if we are expecting to send back a response
                if (msg.content.addressableInvocation.invocationType == AddressableInvocationType.REQUEST_RESPONSE) {
                    // We are about to register a listener that will propagate errors back to the original caller.
                    // So errors can now be considered handled.
                    context.suppressErrors = true

                    context.completion.invokeOnCompletion { exception ->
                        Message(
                            messageId = msg.messageId,
                            target = NetTarget.Unicast(msg.source!!),
                            content = if (exception != null) {
                                MessageContent.ResponseErrorMessage(exception)
                            } else {
                                @UseExperimental(ExperimentalCoroutinesApi::class)
                                MessageContent.ResponseNormalMessage(context.completion.getCompleted())
                            }
                        ).also {
                            context.newOutbound(it)
                        }
                    }
                }

                context.nextInbound(msg)
            }

            else -> Unit
        }
    }
}