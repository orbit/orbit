/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline

import cloud.orbit.common.exception.CapacityExceededException
import cloud.orbit.common.logging.logger
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.net.DirectionalMessage
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.remoting.RemoteInvocation
import cloud.orbit.runtime.stage.ErrorHandler
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class PipelineManager(
    private val supervisorScope: SupervisorScope,
    private val stageConfig: StageConfig,
    private val errorHandler: ErrorHandler
) {
    private val logger by logger()

    private lateinit var pipelineChannel: Channel<DirectionalMessage>
    private lateinit var pipelinesWorkers: List<Job>

    fun writeInvocation(remoteInvocation: RemoteInvocation) {
        val msg = DirectionalMessage.OutboundMessage(
            MessageContent.InvocationRequest(
                completion = remoteInvocation.completion,
                remoteInvocation = remoteInvocation
            )
        )

        // Offer the content to the channel
        if (!pipelineChannel.offer(msg)) {
            // If the channel rejected there must be no capacity, we complete the deferred result exceptionally.
            msg.content.completion.completeExceptionally(
                CapacityExceededException(
                    "The Orbit pipeline channel is full. >${stageConfig.pipelineBufferCount} buffered messages."
                )
            )
        }
    }

    fun start() {
        pipelineChannel = Channel(stageConfig.pipelineBufferCount)
        pipelinesWorkers = List(stageConfig.pipelineWorkerCount) {
            launchWorker(pipelineChannel)
        }

        logger.info(
            "Started ${stageConfig.pipelineWorkerCount} workers with a " +
                    "${stageConfig.pipelineBufferCount} content buffer."
        )
    }

    private fun launchWorker(receiveChannel: ReceiveChannel<DirectionalMessage>) = supervisorScope.launch {
        for (msg in receiveChannel) {
            try {
                onMessage(msg)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                errorHandler.onUnhandledException(e)
            }
        }
    }

    private suspend fun onMessage(message: DirectionalMessage) {
        logger.info(message.toString())
    }

    fun stop() {
        pipelineChannel.close()
        pipelinesWorkers.forEach(Job::cancel)
    }
}