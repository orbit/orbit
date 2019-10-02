/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.server.OrbitServerConfig
import orbit.server.concurrent.RuntimeScopes
import orbit.server.net.Completion
import orbit.server.net.MessageContainer
import orbit.shared.exception.CapacityExceededException
import orbit.shared.net.Message

class Pipeline(
    private val config: OrbitServerConfig,
    private val runtimeScopes: RuntimeScopes,
    private val pipelineSteps: PipelineSteps
) {
    private val logger = KotlinLogging.logger {}

    private lateinit var pipelineChannel: Channel<MessageContainer>
    private lateinit var pipelinesWorkers: List<Job>

    fun start() {
        pipelineChannel = Channel(config.pipelineBufferCount)
        pipelinesWorkers = List(config.pipelineRailCount) {
            launchRail(pipelineChannel)
        }

        logger.info(
            "Pipeline started on ${config.pipelineRailCount} rails with a " +
                    "${config.pipelineBufferCount} entries buffer."
        )
    }

    fun stop() {
        pipelineChannel.close()
        pipelinesWorkers.forEach {
            it.cancel()
        }
    }

    fun writeMessage(message: Message): Completion {
        check(this::pipelineChannel.isInitialized) { "The Orbit pipeline is not initialized." }

        val completion = CompletableDeferred<Unit>()

        val container = MessageContainer(
            message = message,
            completion = completion
        )

        logger.trace { "Writing message to pipeline channel: $message" }

        // Offer the content to the channel
        try {
            if (!pipelineChannel.offer(container)) {
                // If the channel rejected there must be no capacity, we complete the deferred result exceptionally.
                val errMsg = "The Orbit pipeline channel is full. >${config.pipelineBufferCount} buffered messages."
                logger.error(errMsg)
                completion.completeExceptionally(
                    CapacityExceededException(errMsg)
                )
            }
        } catch (t: Throwable) {
            error("The pipeline channel is closed")
        }

        return completion
    }

    private fun launchRail(receiveChannel: ReceiveChannel<MessageContainer>) = runtimeScopes.cpuScope.launch {
        for (msg in receiveChannel) {
            logger.trace { "Pipeline rail received message: $msg" }
            onMessage(msg)
        }
    }

    private suspend fun onMessage(messageContainer: MessageContainer) {
        val context = PipelineContext(
            pipelineSteps = pipelineSteps.steps,
            pipeline = this,
            completion = messageContainer.completion
        )

        try {
            context.next(messageContainer.message)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            messageContainer.completion.completeExceptionally(t)
        }
    }
}