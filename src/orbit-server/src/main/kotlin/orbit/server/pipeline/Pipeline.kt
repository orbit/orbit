/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.server.OrbitServerConfig
import orbit.server.concurrent.RuntimeScopes
import orbit.shared.exception.CapacityExceededException
import orbit.shared.net.Message
import orbit.shared.proto.Completion

class Pipeline(
    private val config: OrbitServerConfig,
    private val runtimeScopes: RuntimeScopes
) {
    private val logger = KotlinLogging.logger {}

    private lateinit var pipelineChannel: Channel<Message>
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

        val completion = CompletableDeferred<Any?>()

        logger.trace { "Writing message to pipeline channel: $message" }

        // Offer the content to the channel
        try {
            if (!pipelineChannel.offer(message)) {
                // If the channel rejected there must be no capacity, we complete the deferred result exceptionally.
                val errMsg = "The Orbit pipeline channel is full. >${config.pipelineBufferCount} buffered messages."
                logger.error(errMsg)
                completion.completeExceptionally(
                    CapacityExceededException(errMsg)
                )
            }
        }catch(t: Throwable) {
            error("The pipeline channel is closed")
        }


        return completion
    }

    private fun launchRail(receiveChannel: ReceiveChannel<Message>) = runtimeScopes.cpuScope.launch {
        for (msg in receiveChannel) {
            logger.trace { "Pipeline rail received message: $msg" }
            onMessage(msg)
        }
    }

    private fun onMessage(message: Message) {

    }
}