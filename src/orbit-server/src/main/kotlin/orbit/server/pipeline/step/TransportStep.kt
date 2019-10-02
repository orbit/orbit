/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.net.ConnectionManager
import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget

class TransportStep(
    private val connectionManager: ConnectionManager
) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        when (val target = msg.target) {
            is MessageTarget.Unicast -> {
                checkNotNull(
                    connectionManager.getClient(target.targetNode)?.sendMessage(msg)
                ) {
                    "Can't find target node ${msg.target}"
                }
            }

            else -> {
                error("Target could not be resolved ${msg.target}")
            }
        }
    }
}