/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.steps

import orbit.server.net.Message
import orbit.server.net.MessageContent
import orbit.server.net.MessageTarget
import orbit.server.pipeline.PipelineContext

internal class ErrorPipelineStep : PipelineStep {

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        try {
            super.onInbound(context, msg)
        } catch (exception: Throwable) {
            context.newOutbound(
                Message(
                    messageId = msg.messageId,
                    target = MessageTarget.Unicast(msg.source!!),
                    content = MessageContent.ResponseErrorMessage(exception)
                )
            )
        }
    }

    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        super.onOutbound(context, msg)
    }
}
