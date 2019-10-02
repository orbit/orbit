/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message

class BlankPipelineStep : PipelineStep {
    override suspend fun next(context: PipelineContext, msg: Message) {
        println("HERE")
        super.next(context, msg)
    }
}