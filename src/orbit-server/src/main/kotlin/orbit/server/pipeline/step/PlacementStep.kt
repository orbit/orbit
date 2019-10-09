/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.mesh.AddressableManager
import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget

class PlacementStep(
    private val addressableManager: AddressableManager
) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when (val content = msg.content) {
            is MessageContent.InvocationRequest -> {
                addressableManager.placeOrLocate(msg.source!!.namespace, content.destination).also { location ->
                    msg.copy(
                        target = MessageTarget.Unicast(location)
                    ).also { newMsg ->
                        context.next(newMsg)
                    }
                }
            }
            else -> context.next(msg)
        }
    }
}