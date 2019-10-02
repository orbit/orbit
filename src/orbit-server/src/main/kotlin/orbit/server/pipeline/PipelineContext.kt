/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import kotlinx.coroutines.isActive
import orbit.server.net.Completion
import orbit.server.pipeline.step.PipelineStep
import orbit.shared.net.Message
import java.util.concurrent.CancellationException
import kotlin.coroutines.coroutineContext

class PipelineContext(
    private val pipelineSteps: Array<PipelineStep>,
    private val pipeline: Pipeline,
    val completion: Completion
) {
    private val pipelineSize = pipelineSteps.size
    private var pointer = -1

    suspend fun next(msg: Message) {
        if (!coroutineContext.isActive) throw CancellationException()

        check(++pointer < pipelineSize) { "End of pipeline encountered." }

        val pipelineStep = pipelineSteps[pointer]
        pipelineStep.next(this, msg)
    }
}