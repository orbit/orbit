/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline

import cloud.orbit.common.exception.CapacityExceededException
import cloud.orbit.common.logging.logger
import cloud.orbit.common.logging.trace
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.runtime.concurrent.RuntimeScopes
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContainer
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.net.MessageDirection
import cloud.orbit.runtime.pipeline.steps.ExecutionStep
import cloud.orbit.runtime.pipeline.steps.IdentityStep
import cloud.orbit.runtime.pipeline.steps.LoopbackStep
import cloud.orbit.runtime.pipeline.steps.PipelineStep
import cloud.orbit.runtime.pipeline.steps.ResponseTrackingStep
import cloud.orbit.runtime.pipeline.steps.RoutingStep
import cloud.orbit.runtime.pipeline.steps.SerializationStep
import cloud.orbit.runtime.pipeline.steps.TransportStep
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

internal class PipelineSystem(
    private val componentProvider: ComponentProvider
) {
    private val pipelineStepConfig = listOf(
        ExecutionStep::class.java,
        IdentityStep::class.java,
        RoutingStep::class.java,
        ResponseTrackingStep::class.java,
        LoopbackStep::class.java,
        SerializationStep::class.java,
        TransportStep::class.java
    )

    private val logger by logger()
    private val runtimeScopes: RuntimeScopes by componentProvider.inject()
    private val stageConfig: StageConfig by componentProvider.inject()


    private lateinit var pipelineChannel: Channel<MessageContainer>
    private lateinit var pipelinesWorkers: List<Job>
    private lateinit var pipelineSteps: Array<PipelineStep>


    fun start() {
        pipelineChannel = Channel(stageConfig.pipelineBufferCount)
        pipelinesWorkers = List(stageConfig.pipelineRailCount) {
            launchRail(pipelineChannel)
        }
        pipelineSteps = pipelineStepConfig.map(componentProvider::construct).toTypedArray()

        logger.info(
            "Pipeline started on ${stageConfig.pipelineRailCount} rails with a " +
                    "${stageConfig.pipelineBufferCount} entries buffer and ${pipelineSteps.size} steps."
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
            val errMsg = "The Orbit pipeline channel is full. >${stageConfig.pipelineBufferCount} buffered messages."
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

    fun pushInvocation(addressableInvocation: AddressableInvocation, target: NetTarget? = null) =
        pushOutbound(
            Message(
                content = MessageContent.RequestInvocationMessage(addressableInvocation),
                target = target
            )
        )


    private suspend fun onMessage(container: MessageContainer) {
        val startAtEnd = container.direction == MessageDirection.INBOUND
        val errorsAreHandled = container.direction == MessageDirection.OUTBOUND

        val context = PipelineContext(
            pipeline = pipelineSteps,
            startAtEnd = startAtEnd,
            pipelineSystem = this,
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
            if (context.suppressErrors) {
                container.completion.completeExceptionally(t)
            } else {
                throw t
            }
        }
    }


    fun stop() {
        pipelineChannel.close()
        pipelinesWorkers.forEach(Job::cancel)
    }
}