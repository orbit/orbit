/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.steps

import orbit.server.net.Message
import orbit.server.net.MessageContent
import orbit.server.net.MessageTarget
import orbit.server.pipeline.PipelineContext
import orbit.server.routing.AddressableDirectory
import orbit.server.routing.AddressablePlacementStrategy

internal class AddressablePipelineStep(
    private val addressableDirectory: AddressableDirectory,
    private val addressablePlacement: AddressablePlacementStrategy
) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when {
            msg.content is MessageContent.Request -> {
                val destination = msg.content.destination

                (addressableDirectory.getLease(destination)?.nodeId
                    ?: addressablePlacement.chooseNode(destination)).let { nodeId ->
                    println("Addressable inbound: $nodeId")
                    context.nextInbound(
                        msg.copy(
                            target = MessageTarget.Unicast(nodeId),
                            content = msg.content
                        )
                    )
                }
            }
            else -> context.nextInbound(msg)
        }

    }

    override suspend fun onOutbound(context: PipelineContext, msg: Message) {

    }
}