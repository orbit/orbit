/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.mesh.AddressableManager
import orbit.server.mesh.LocalNodeInfo
import orbit.server.pipeline.PipelineContext
import orbit.shared.net.InvocationReason
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget

class PlacementStep(
    private val addressableManager: AddressableManager,
    private val localNodeInfo: LocalNodeInfo
) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        when (val content = msg.content) {
            is MessageContent.InvocationRequest -> {
                val ineligibleNodes =
                    if (content.reason == InvocationReason.rerouted) listOf(context.metadata.authInfo.nodeId) else emptyList()

                addressableManager.locateOrPlace(msg.source!!.namespace, content.destination, ineligibleNodes).also { location ->
                    msg.copy(
                        target = MessageTarget.Unicast(location)
                    ).also { newMsg ->
                        context.next(newMsg)
                    }
                }
            }
            is MessageContent.ConnectionInfoRequest ->
                msg.source?.also { source ->
                    msg.copy(
                        target = MessageTarget.Unicast(source),
                        content = MessageContent.ConnectionInfoResponse(
                            nodeId = localNodeInfo.info.id
                        )
                    ).also {
                        context.pushNew(it)
                    }
                }
            else -> context.next(msg)
        }
    }
}