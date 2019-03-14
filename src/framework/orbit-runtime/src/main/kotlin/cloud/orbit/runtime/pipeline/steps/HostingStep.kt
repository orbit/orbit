/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.runtime.hosting.HostingManager
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageTarget
import cloud.orbit.runtime.net.MessageType
import cloud.orbit.runtime.pipeline.PipelineContext

class HostingStep(private val hostingManager: HostingManager) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        val newMsg = when(msg.messageType) {
            MessageType.INVOCATION_REQUEST -> {
                val targetNode = hostingManager.locateOrPlace(msg.remoteInvocation!!.target)
                msg.copy(
                    target = MessageTarget.Unicast(targetNode)
                )
            }
            else ->  msg
        }
        context.nextOutbound(newMsg)
    }

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        context.nextInbound(msg)
    }
}