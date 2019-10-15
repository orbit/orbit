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
        val targetNode = when (val target = msg.target) {
            is MessageTarget.Unicast -> {
                target.targetNode
            }

            is MessageTarget.RoutedUnicast -> target.route.nextNode

            else -> null
        }

        checkNotNull(targetNode) { "Could not determine a target ${msg.target}" }

        val client = connectionManager.getClient(targetNode)

        checkNotNull(client) { "Could not find target $targetNode in connections" }

        client.sendMessage(msg)
    }
}