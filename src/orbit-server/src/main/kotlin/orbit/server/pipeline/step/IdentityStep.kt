/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.mesh.LocalNodeInfo
import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message
import java.util.concurrent.atomic.AtomicLong

class IdentityStep(
    private val localNodeInfo: LocalNodeInfo
) : PipelineStep {
    private val messageCounter = AtomicLong(0)

    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        msg.copy(
            messageId = msg.messageId ?: messageCounter.incrementAndGet(),
            source = msg.source ?: localNodeInfo.info.id
        ).also {
            context.next(it)
        }
    }
}