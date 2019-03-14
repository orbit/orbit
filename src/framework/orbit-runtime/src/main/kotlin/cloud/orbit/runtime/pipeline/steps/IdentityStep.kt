/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.common.util.RandomUtils
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.NetManager
import cloud.orbit.runtime.pipeline.PipelineContext

class IdentityStep(
    private val netManager: NetManager
) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
       var newMsg = msg

        if(newMsg.messageId == null) {
            newMsg = newMsg.copy(
                messageId = RandomUtils.sequentialId()
            )
        }

        if(newMsg.source == null) {
            newMsg = newMsg.copy(
                source = netManager.localNode.nodeIdentity
            )
        }

        context.nextOutbound(newMsg)
    }
}