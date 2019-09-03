/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.steps

import orbit.server.Address
import orbit.server.AddressId
import orbit.server.net.Message
import orbit.server.net.MessageContent
import orbit.server.net.MessageTarget
import orbit.server.pipeline.PipelineContext
import orbit.server.routing.AddressableDirectory
import orbit.server.routing.AddressablePlacementStrategy
import orbit.shared.proto.Messages

internal class AddressablePipelineStep(
    private val addressableDirectory: AddressableDirectory,
    private val addressablePlacement: AddressablePlacementStrategy
) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        println("Addressable inbound")
        when {
            msg.content is MessageContent.Request -> {
                val destination = msg.content.destination

                (addressableDirectory.lookup(destination) ?: addressablePlacement.chooseNode(destination)).let { nodeId ->
                    if (nodeId == null) {
                        return@let msg
                    }
                    msg.copy(
                        target = MessageTarget.Unicast(nodeId),
                        content = msg.content
                    )
                }
            }
        }

    }

    override suspend fun onOutbound(context: PipelineContext, msg: Message) {

    }
}