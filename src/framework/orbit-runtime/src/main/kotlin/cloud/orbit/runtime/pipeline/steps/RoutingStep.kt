/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.runtime.hosting.RoutingSystem
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.pipeline.PipelineContext

internal class RoutingStep(private val routingSystem: RoutingSystem) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        val newMsg = when(msg.content) {
            is MessageContent.RequestInvocationMessage -> {
                val target = routingSystem.routeMessage(msg.content.addressableInvocation.reference, msg.target)
                msg.copy(
                    target = target
                )
            }
            else ->  msg
        }
        context.nextOutbound(newMsg)
    }

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when(msg.content) {
            is MessageContent.RequestInvocationMessage -> {
                if(!routingSystem.canHandleLocally(msg.content.addressableInvocation.reference)) {
                    // Can't handle locally so we just start it as a new call
                    context.newOutbound(msg)
                    return
                }
            }
        }
        context.nextInbound(msg)
    }
}