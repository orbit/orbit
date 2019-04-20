/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.core.net.NetTarget
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.Networking
import cloud.orbit.runtime.pipeline.PipelineContext
import cloud.orbit.runtime.serialization.Serialization
import cloud.orbit.runtime.stage.StageConfig

internal class LoopbackStep(
    private val serialization: Serialization,
    private val networking: Networking,
    private val stageConfig: StageConfig
) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {

        if (stageConfig.allowLoopback
            && msg.target is NetTarget.Unicast
            && msg.target.targetNode == networking.localNode.nodeIdentity
        ) {
            serialization.cloneObject(msg.content).let {
                msg.copy(
                    content = it
                )
            }.also {
                context.nextInbound(it)
            }
        } else {
            context.nextOutbound(msg)
        }
    }
}