/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.core.net.NetTarget
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.pipeline.PipelineContext
import cloud.orbit.runtime.serialization.SerializationSystem
import cloud.orbit.runtime.stage.StageConfig

internal class LoopbackStep(
    private val serializationSystem: SerializationSystem,
    private val netSystem: NetSystem,
    private val stageConfig: StageConfig
) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {

        if (stageConfig.allowLoopback
            && msg.target is NetTarget.Unicast
            && msg.target.targetNode == netSystem.localNode.nodeIdentity
        ) {
            serializationSystem.cloneObject(msg.content).let {
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