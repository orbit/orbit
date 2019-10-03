/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message

class AuthStep : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        val newSource = msg.source.let { src ->
            // If there is no source we set it
            // We also can't trust clients so we check the namespace
            if (src == null || !context.metadata.authInfo.isManagementNode) {
                context.metadata.authInfo.nodeId
            } else {
                src
            }
        }

        val newMsg = msg.copy(
            source = newSource
        )

        context.next(newMsg)
    }
}