/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message

interface PipelineStep {
    suspend fun onOutbound(context: PipelineContext, msg: Message): Unit = context.next(msg)
    suspend fun onInbound(context: PipelineContext, msg: Message): Unit = context.next(msg)
}