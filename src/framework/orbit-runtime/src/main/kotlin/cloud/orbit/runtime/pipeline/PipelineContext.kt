/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline

import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.pipeline.steps.PipelineStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class PipelineContext(
    private val pipeline: Array<PipelineStep>,
    startAtEnd: Boolean,
    private val pipelineSystem: PipelineSystem,
    val completion: Completion
) {
    private val pipelineSize = pipeline.size
    private var pointer = if (startAtEnd) pipelineSize else -1

    suspend fun nextInbound(msg: Message) {
        if (!coroutineContext.isActive) throw CancellationException()
        if (--pointer < 0) throw IllegalStateException("Beginning of pipeline encountered.")
        val pipelineStep = pipeline[pointer]
        pipelineStep.onInbound(this, msg)

    }

    suspend fun nextOutbound(msg: Message) {
        if (!coroutineContext.isActive) throw CancellationException()
        if (++pointer >= pipelineSize) throw IllegalStateException("End of pipeline encountered.")
        val pipelineStep = pipeline[pointer]
        pipelineStep.onOutbound(this, msg)

    }

    fun newInbound(msg: Message) =
        pipelineSystem.pushInbound(msg)

    fun newOutbound(msg: Message) =
        pipelineSystem.pushOutbound(msg)
}