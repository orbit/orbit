/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline

import cloud.orbit.common.exception.CapacityExceededException
import cloud.orbit.common.logging.logger
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContainer
import cloud.orbit.runtime.net.MessageDirection
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.pipeline.steps.PipelineStep
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class PipelineSystem(
    private val componentProvider: ComponentProvider
) {
    private val logger by logger()
    private val supervisorScope: SupervisorScope by componentProvider.inject()
    private val stageConfig: StageConfig by componentProvider.inject()
    private val netSystem: NetSystem by componentProvider.inject()


    private lateinit var pipelineChannel: Channel<MessageContainer>
    private lateinit var pipelinesWorkers: List<Job>
    private lateinit var pipelineSteps: List<PipelineStep>


    fun start() {
        pipelineChannel = Channel(stageConfig.pipelineBufferCount)
        pipelinesWorkers = List(stageConfig.pipelineRailCount) {
            launchWorker(pipelineChannel)
        }
        pipelineSteps = stageConfig.pipelineStepsDefinition.map(componentProvider::construct)

        logger.info(
            "Pipeline started on ${stageConfig.pipelineRailCount} rails with a " +
                    "${stageConfig.pipelineBufferCount} entries buffer and ${pipelineSteps.size} steps."
        )
    }

    private fun launchWorker(receiveChannel: ReceiveChannel<MessageContainer>) = supervisorScope.launch {
        for (msg in receiveChannel) {
            try {
                onMessage(msg)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                msg.completion.completeExceptionally(t)
            }
        }
    }

    private fun writeMessage(msg: Message, direction: MessageDirection): CompletableDeferred<Any?> {
        val completion = CompletableDeferred<Any?>()

        val container = MessageContainer(
            direction = direction,
            completion = completion,
            msg = msg
        )

        // Offer the content to the channel
        if (!pipelineChannel.offer(container)) {
            // If the channel rejected there must be no capacity, we complete the deferred result exceptionally.
            completion.completeExceptionally(
                CapacityExceededException(
                    "The Orbit pipeline channel is full. >${stageConfig.pipelineBufferCount} buffered messages."
                )
            )
        }

        return container.completion
    }

    fun pushOutbound(msg: Message) =
        writeMessage(msg, MessageDirection.OUTBOUND)

    fun pushInbound(msg: Message) =
        writeMessage(msg, MessageDirection.INBOUND)


    private suspend fun onMessage(container: MessageContainer) {
        when (container.direction) {
            MessageDirection.OUTBOUND ->
                PipelineContext(pipelineSteps, false, this, container.completion)
                    .nextOutbound(container.msg)
            MessageDirection.INBOUND ->
                PipelineContext(pipelineSteps, true, this, container.completion)
                    .nextInbound(container.msg)
        }
    }


    fun stop() {
        pipelineChannel.close()
        pipelinesWorkers.forEach(Job::cancel)
    }
}