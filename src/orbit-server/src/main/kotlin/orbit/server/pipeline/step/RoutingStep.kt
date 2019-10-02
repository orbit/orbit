/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.pipeline.PipelineContext
import orbit.server.router.Router
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget

class RoutingStep(
    private val router: Router
) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        checkNotNull(msg.target) { "Target may not be null for outbound messages. $msg" }
        val route = when (val target = msg.target) {
            is MessageTarget.Unicast -> {
                router.findRoute(target.targetNode)
            }
            is MessageTarget.RoutedUnicast -> router.findRoute(target.route.destinationNode, target.route)
            else -> null
        }

        checkNotNull(route) { "Could not find route for $msg" }

        val newMsg = msg.copy(
            target = MessageTarget.RoutedUnicast(route)
        )

        context.nextOutbound(newMsg)
    }
}