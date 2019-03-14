/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.runtime.hosting.ResponseTracking
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageType
import cloud.orbit.runtime.pipeline.PipelineContext

class ResponseTrackingStep(private val responseTracking: ResponseTracking) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        when (msg.messageType) {
            MessageType.INVOCATION_REQUEST -> responseTracking.trackMessage(msg, context.completion!!)
            else -> Unit
        }
        context.nextOutbound(msg)
    }

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when (msg.messageType) {
            MessageType.INVOCATION_RESPONSE_ERROR,
            MessageType.INVOCATION_RESPONSE_NORMAL ->
                responseTracking.handleResponse(msg)
            else -> context.nextInbound(msg)
        }
    }
}