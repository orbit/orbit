/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.server.OrbitServerConfig
import orbit.server.auth.AuthInfo
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.LocalNodeInfo
import orbit.server.net.MessageContainer
import orbit.server.net.MessageDirection
import orbit.server.net.MessageMetadata
import orbit.shared.exception.CapacityExceededException
import orbit.shared.exception.toErrorContent
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget
import orbit.util.concurrent.RailWorker

class Pipeline(
    private val config: OrbitServerConfig,
    private val runtimeScopes: RuntimeScopes,
    private val pipelineSteps: PipelineSteps,
    private val localNodeInfo: LocalNodeInfo
) {
    private val logger = KotlinLogging.logger {}

    private val pipelineRails = RailWorker(
        scope = runtimeScopes.cpuScope,
        buffer = config.pipelineBufferCount,
        railCount = config.pipelineRailCount,
        logger = logger,
        onMessage = this::onMessage
    )

    fun start() {
        pipelineRails.startWorkers()
    }

    fun stop() {
        pipelineRails.stopWorkers()
    }

    fun pushMessage(msg: Message, meta: MessageMetadata? = null) {
        check(pipelineRails.isInitialized) {
            "The Orbit pipeline is not in a state to receive messages. Did you start the Orbit stage?"
        }

        val container = MessageContainer(
            message = msg,
            metadata = meta ?: localMeta
        )

        logger.trace { "Writing message to pipeline channel: $container" }

        // Offer the content to the channel
        try {
            if (!pipelineRails.offer(container)) {
                // If the channel rejected there must be no capacity, we complete the deferred result exceptionally.
                val errMsg = "The Orbit pipeline channel is full. >${config.pipelineBufferCount} buffered messages."
                logger.error(errMsg)
                throw CapacityExceededException(errMsg)
            }
        } catch (t: Throwable) {
            error("The pipeline channel is closed")
        }
    }

    private val localMeta
        get() = MessageMetadata(
            messageDirection = MessageDirection.OUTBOUND,
            authInfo = AuthInfo(true, localNodeInfo.info.namespace, localNodeInfo.info.id),
            respondOnError = true
        )

    private fun launchRail(receiveChannel: ReceiveChannel<MessageContainer>) = runtimeScopes.cpuScope.launch {
        for (msg in receiveChannel) {
            logger.trace { "Pipeline rail received message: $msg" }
            onMessage(msg)
        }
    }

    private suspend fun onMessage(container: MessageContainer) {
        val context = PipelineContext(
            pipelineSteps = pipelineSteps.steps,
            pipeline = this,
            metadata = container.metadata
        )

        try {
            context.next(container.message)
        } catch (t: PipelineException) {
            if (container.metadata.respondOnError) {
                val src = t.lastMsgState.source ?: container.metadata.authInfo.nodeId

                val newMessage = Message(
                    messageId = container.message.messageId,
                    target = MessageTarget.Unicast(src),
                    content = t.reason.toErrorContent()
                )

                val newMeta = localMeta.copy(respondOnError = false)

                pushMessage(newMessage, newMeta)
            } else {
                throw t.reason
            }
        } catch (c: CancellationException) {

            throw c

        }
    }
}