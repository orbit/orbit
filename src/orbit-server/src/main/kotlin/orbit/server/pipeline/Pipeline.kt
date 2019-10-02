/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.server.OrbitServerConfig
import orbit.server.concurrent.RuntimeScopes
import orbit.server.net.MessageContainer
import orbit.server.net.MessageDirection
import orbit.shared.exception.CapacityExceededException
import orbit.shared.exception.toErrorContent
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget

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

    private fun writeMessage(msg: Message, direction: MessageDirection) {
        check(this::pipelineChannel.isInitialized) {
            "The Orbit pipeline is not in a state to receive messages. Did you start the Orbit stage?"
        }

        val container = MessageContainer(
            direction = direction,
            message = msg
        )

        logger.trace { "Writing message to pipeline channel: $container" }

        // Offer the content to the channel
        try {
            if (!pipelineChannel.offer(container)) {
                // If the channel rejected there must be no capacity, we complete the deferred result exceptionally.
                val errMsg = "The Orbit pipeline channel is full. >${config.pipelineBufferCount} buffered messages."
                logger.error(errMsg)
                throw CapacityExceededException(errMsg)
            }
        } catch (t: Throwable) {
            error("The pipeline channel is closed")
        }
    }

    private fun launchRail(receiveChannel: ReceiveChannel<MessageContainer>) = runtimeScopes.cpuScope.launch {
        for (msg in receiveChannel) {
            logger.trace { "Pipeline rail received message: $msg" }
            onMessage(msg)
        }
    }

    private suspend fun onMessage(container: MessageContainer, respondOnError: Boolean = true) {
        // Inbound starts at bottom, outbound at top.
        val startAtEnd = container.direction == MessageDirection.INBOUND

        val context = PipelineContext(
            pipelineSteps = pipelineSteps.steps,
            startAtEnd = startAtEnd,
            pipeline = this
        )

        try {
            when (container.direction) {
                MessageDirection.OUTBOUND -> context.nextOutbound(container.message)
                MessageDirection.INBOUND -> context.nextInbound(container.message)
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            if (respondOnError && container.message.source != null) {
                val errMsg = MessageContainer(
                    direction = MessageDirection.OUTBOUND,
                    message = Message(
                        messageId = container.message.messageId,
                        target = MessageTarget.Unicast(container.message.source!!),
                        content = t.toErrorContent()
                    )
                )
                onMessage(errMsg, false)
            } else {
                throw t
            }
        }
    }
}