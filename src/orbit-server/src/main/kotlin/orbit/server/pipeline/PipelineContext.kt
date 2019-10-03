/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import kotlinx.coroutines.isActive
import orbit.server.net.MessageDirection
import orbit.server.net.MessageMetadata
import orbit.server.pipeline.step.PipelineStep
import orbit.shared.net.Message
import java.util.concurrent.CancellationException
import kotlin.coroutines.coroutineContext

class PipelineContext(
    private val pipelineSteps: Array<PipelineStep>,
    private val pipeline: Pipeline,
    val metadata: MessageMetadata
) {
    private val pipelineSize = pipelineSteps.size
    private var pointer = when (metadata.messageDirection) {
        MessageDirection.INBOUND -> {
            pipelineSize
        }
        MessageDirection.OUTBOUND -> {
            -1
        }
    }

    suspend fun next(msg: Message) {
        try {
            when (metadata.messageDirection) {
                MessageDirection.INBOUND -> {
                    if (!coroutineContext.isActive) throw CancellationException()
                    check(--pointer >= 0) { "Beginning of pipeline encountered." }
                    val pipelineStep = pipelineSteps[pointer]
                    pipelineStep.onInbound(this, msg)
                }
                MessageDirection.OUTBOUND -> {
                    if (!coroutineContext.isActive) throw CancellationException()
                    check(++pointer < pipelineSize) { "End of pipeline encountered." }
                    val pipelineStep = pipelineSteps[pointer]
                    pipelineStep.onOutbound(this, msg)
                }
            }
        } catch (t: PipelineException) {
            throw t
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            throw PipelineException(msg, t)
        }
    }

    fun pushNew(msg: Message, newMeta: MessageMetadata? = null) =
        pipeline.pushMessage(msg, newMeta)
}