/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.runtime.hosting.ExecutionSystem
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.pipeline.PipelineContext

class ExecutionStep(
    private val executionSystem: ExecutionSystem
) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when (msg.content) {
            is MessageContent.RequestInvocationMessage ->
                executionSystem.handleInvocation(msg.content.remoteInvocation, context.completion!!)
            else -> context.nextInbound(msg)
        }
    }
}