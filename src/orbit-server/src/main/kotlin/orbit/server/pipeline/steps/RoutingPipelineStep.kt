/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.steps

import orbit.server.net.Message
import orbit.server.net.MessageTarget
import orbit.server.net.NodeCollection
import orbit.server.pipeline.PipelineContext
import orbit.server.routing.Router

internal class RoutingPipelineStep(private val router: Router, private val nodeCollection: NodeCollection) :
    PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        println("Routing inbound")
        when {
            msg.target is MessageTarget.Unicast ->
                router.getRoute(msg.target.targetNode).let { route ->
                    if (route == null) {
                        return@let msg
                    }
                    context.newOutbound(msg.copy(
                        target = MessageTarget.Routed(route),
                        content = msg.content
                    ))
                }
            else -> context.nextInbound(msg)
        }
    }

    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        println("Routing outbound")
        when {
            msg.target is MessageTarget.Unicast -> {
                nodeCollection.getNode(msg.target.targetNode)?.sendMessage(msg)
            }
            msg.target is MessageTarget.Routed -> {
                nodeCollection.getNode(msg.target.route.nextNode)?.sendMessage(msg, msg.target.route)
            }
        }
    }
}