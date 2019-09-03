/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.steps

import orbit.server.net.Message
import orbit.server.net.MessageTarget
import orbit.server.pipeline.PipelineContext
import orbit.server.routing.Router

internal class RoutingPipelineStep(private val router: Router) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when {
            msg.target is MessageTarget.Unicast ->
                router.getRoute(msg.target.targetNode).let { route ->
                    if (route == null) {
                        return@let msg
                    }
                    msg.copy(
                        target = MessageTarget.Routed(route),
                        content = msg.content
                    )
                }
        }
    }

    override suspend fun onOutbound(context: PipelineContext, msg: Message) {

    }
}