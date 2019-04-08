/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.common.util.RandomUtils
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.pipeline.PipelineContext

internal class IdentityStep(
    private val netSystem: NetSystem
) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        msg.copy(
            messageId = msg.messageId ?: RandomUtils.sequentialId(),
            source = msg.source ?: netSystem.localNode.nodeIdentity
        ).also {
            context.nextOutbound(it)
        }
    }
}