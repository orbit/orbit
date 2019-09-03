/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import orbit.common.exception.CapacityExceededException
import orbit.common.logging.logger
import orbit.common.logging.trace
import orbit.server.OrbitServerConfig
import orbit.server.concurrent.RuntimeScopes
import orbit.server.net.Message
import orbit.server.net.MessageContainer
import orbit.server.net.MessageDirection
import orbit.server.pipeline.steps.AddressablePipelineStep
import orbit.server.pipeline.steps.BlankPipelineStep
import orbit.server.pipeline.steps.PipelineStep
import orbit.server.pipeline.steps.RoutingPipelineStep
import org.kodein.di.DKodein
import org.kodein.di.erased.instance

internal class Pipeline(
    private val runtimeScopes: RuntimeScopes,
    private val config: OrbitServerConfig,
    kodein: DKodein
) {
    private val logger by logger()

    private lateinit var pipelineChannel: Channel<MessageContainer>
    private lateinit var pipelinesWorkers: List<Job>

    private val pipelineSteps: Array<PipelineStep> = arrayOf(
        kodein.instance<RoutingPipelineStep>(),
        kodein.instance<AddressablePipelineStep>()
    )

    fun start() {
        pipelineChannel = Channel(config.pipelineBufferCount)
        pipelinesWorkers = List(config.pipelineRailCount) {
            launchRail(pipelineChannel)
        }

        logger.info(
            "Pipeline started on ${config.pipelineRailCount} rails with a " +
                    "${config.pipelineBufferCount} entries buffer and ${pipelineSteps.size} steps."
        )
    }

    private fun launchRail(receiveChannel: ReceiveChannel<MessageContainer>) = runtimeScopes.cpuScope.launch {
        for (container in receiveChannel) {
            logger.trace { "Pipeline rail received message: $container" }
            onMessage(container)
        }
    }

    private fun writeMessage(msg: Message, direction: MessageDirection): CompletableDeferred<Any?> {
        @UseExperimental(ExperimentalCoroutinesApi::class)
        if (!this::pipelineChannel.isInitialized || pipelineChannel.isClosedForSend) {
            throw IllegalStateException(
                "The Orbit pipeline is not in a state to receive messages. " +
                        "Did you start the Orbit stage?"
            )
        }

        val completion = CompletableDeferred<Any?>()

        val container = MessageContainer(
            direction = direction,
            completion = completion,
            msg = msg
        )

        logger.trace { "Writing message to pipeline channel: $container" }

        // Offer the content to the channel
        if (!pipelineChannel.offer(container)) {
            // If the channel rejected there must be no capacity, we complete the deferred result exceptionally.
            val errMsg = "The Orbit pipeline channel is full. >${config.pipelineBufferCount} buffered messages."
            logger.error(errMsg)
            completion.completeExceptionally(
                CapacityExceededException(errMsg)
            )
        }

        return container.completion
    }

    fun pushOutbound(msg: Message) =
        writeMessage(msg, MessageDirection.OUTBOUND)

    fun pushInbound(msg: Message) =
        writeMessage(msg, MessageDirection.INBOUND)

    private suspend fun onMessage(container: MessageContainer) {
        // Inbound starts at bottom, outbound at top.
        val startAtEnd = container.direction == MessageDirection.INBOUND

        // Outbound messages have a listener (the caller) so errors are considered handled by default.
        // Inbound messages have no listener until later in the pipeline so are considered unhandled by default.
        val errorsAreHandled = container.direction == MessageDirection.OUTBOUND

        val context = PipelineContext(
            pipelineSteps = pipelineSteps,
            startAtEnd = startAtEnd,
            pipeline = this,
            completion = container.completion,
            suppressErrors = errorsAreHandled
        )

        try {
            when (container.direction) {
                MessageDirection.OUTBOUND -> context.nextOutbound(container.msg)
                MessageDirection.INBOUND -> context.nextInbound(container.msg)
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


    fun stop() {
        pipelineChannel.close()
        pipelinesWorkers.forEach {
            it.cancel()
        }
    }
}