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
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.server.OrbitServerConfig
import orbit.server.concurrent.RuntimeScopes
import orbit.server.net.Completion
import orbit.server.net.MessageContainer
import orbit.server.net.MessageDirection
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

    fun pushOutbound(msg: Message) =
        writeMessage(msg, MessageDirection.OUTBOUND)

    fun pushInbound(msg: Message) =
        writeMessage(msg, MessageDirection.INBOUND)

    private fun writeMessage(msg: Message, direction: MessageDirection): Completion {
        check(this::pipelineChannel.isInitialized) {
            "The Orbit pipeline is not in a state to receive messages. Did you start the Orbit stage?"
        }

        val completion = CompletableDeferred<Unit>()

        val container = MessageContainer(
            direction = direction,
            completion = completion,
            message = msg
        )

        logger.trace { "Writing message to pipeline channel: $container" }

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

        return container.completion
    }

    private fun launchRail(receiveChannel: ReceiveChannel<MessageContainer>) = runtimeScopes.cpuScope.launch {
        for (msg in receiveChannel) {
            logger.trace { "Pipeline rail received message: $msg" }
            onMessage(msg)
        }
    }

    private suspend fun onMessage(container: MessageContainer) {
        // Inbound starts at bottom, outbound at top.
        val startAtEnd = container.direction == MessageDirection.INBOUND

        // Outbound messages have a listener (the caller) so errors are considered handled by default.
        // Inbound messages have no listener until later in the pipeline so are considered unhandled by default.
        //val errorsAreHandled = container.direction == MessageDirection.OUTBOUND
        // TODO: How does this work for mesh? Disabled for now
        val errorsAreHandled = true

        val context = PipelineContext(
            pipelineSteps = pipelineSteps.steps,
            startAtEnd = startAtEnd,
            pipeline = this,
            completion = container.completion,
            suppressErrors = errorsAreHandled
        )

        try {
            when (container.direction) {
                MessageDirection.OUTBOUND -> context.nextOutbound(container.message)
                MessageDirection.INBOUND -> context.nextInbound(container.message)
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            // We check to see if errors are suppressed (and therefore handled) and raise the event.
            // If not we consider it an unhandled error and throw it to the Orbit root error handling.
            if (context.suppressErrors) {
                container.completion.completeExceptionally(t)
            } else {
                throw t
            }
        }
    }
}