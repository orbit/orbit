/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline

import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.pipeline.steps.PipelineStep

class PipelineContext(
    private val pipeline: List<PipelineStep>,
    startAtEnd: Boolean,
    private val pipelineManager: PipelineManager,
    var completion: Completion?
) {
    private var pointer = if (startAtEnd) pipeline.size  else -1

    suspend fun nextInbound(msg: Message) {
        if (--pointer < 0) throw IllegalStateException("Beginning of pipeline encountered.")
        val pipelineStep = pipeline[pointer]
        pipelineStep.onInbound(this, msg)

    }

    suspend fun nextOutbound(msg: Message) {
        if (++pointer >= pipeline.size) throw IllegalStateException("End of pipeline encountered.")
        val pipelineStep = pipeline[pointer]
        pipelineStep.onOutbound(this, msg)

    }

    fun newInbound(msg: Message) {
        pipelineManager.pushInbound(msg)
    }

    fun newOutbound(msg: Message) {
        pipelineManager.pushOutbound(msg)
    }
}