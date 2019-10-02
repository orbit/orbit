/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget

class EchoStep : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        msg.source?.also { source ->
            msg.copy(
                target = MessageTarget.Unicast(source)
            ).also {
                context.newOutbound(it)
                return
            }
        }
    }
}