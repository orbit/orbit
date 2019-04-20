/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.exception.CapacityExceededException
import cloud.orbit.common.logging.debug
import cloud.orbit.common.logging.logger
import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.stopwatch
import cloud.orbit.core.remoting.AbstractAddressable
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableContext
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.core.runtime.RuntimeContext
import cloud.orbit.runtime.concurrent.RuntimeScopes
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.pipeline.Pipeline
import cloud.orbit.runtime.remoting.AddressableImplDefinition
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicLong

internal class ExecutionHandle(
    val instance: Addressable,
    val reference: AddressableReference,
    val interfaceDefinition: AddressableInterfaceDefinition,
    val implDefinition: AddressableImplDefinition,
    componentProvider: ComponentProvider
) {
    private val clock: Clock by componentProvider.inject()
    private val runtimeScopes: RuntimeScopes by componentProvider.inject()
    private val stageConfig: StageConfig by componentProvider.inject()
    private val runtimeContext: RuntimeContext by componentProvider.inject()
    private val pipeline: Pipeline by componentProvider.inject()

    private val logger by logger()

    val createdTime = clock.currentTime

    @Volatile
    var deactivateNextTick = false

    private val lastActivityAtomic = AtomicLong(createdTime)
    val lastActivity get() = lastActivityAtomic.get()

    private val channel = Channel<EventType>(stageConfig.addressableBufferCount)

    init {
        if (instance is AbstractAddressable) {
            instance.context = AddressableContext(
                reference = reference,
                runtime = runtimeContext
            )
        }
    }

    fun activate(): Completion =
        CompletableDeferred<Any?>().also {
            sendEvent(EventType.ActivateEvent(it))
        }

    fun deactivate(): Completion =
        CompletableDeferred<Any?>().also {
            sendEvent(EventType.DeactivateEvent(it))
        }

    fun invoke(
        invocation: AddressableInvocation
    ): Completion =
        CompletableDeferred<Any?>().also {
            sendEvent(EventType.InvokeEvent(invocation, it))
        }


    private fun sendEvent(eventType: EventType) {
        if (!channel.offer(eventType)) {
            val errMsg = "Buffer capacity exceeded (>${stageConfig.addressableBufferCount}) for $reference"
            logger.error(errMsg)
            throw CapacityExceededException(errMsg)
        }
    }

    private suspend fun onActivate() {
        logger.debug { "Activating $reference..." }
        stopwatch(clock) {
            if (implDefinition.lifecycle.autoActivate) {
                implDefinition.onActivateMethod?.also {
                    DeferredWrappers.wrapCall(it.method.invoke(instance)).await()
                }
            }
        }.also { (elapsed, _) ->
            logger.debug { "Activated $reference in ${elapsed}ms. " }
        }
    }

    private suspend fun onInvoke(invocation: AddressableInvocation): Any? {
        lastActivityAtomic.set(clock.currentTime)
        try {
            return MethodInvoker.invokeDeferred(instance, invocation.method, invocation.args).await()
        } catch (ite: InvocationTargetException) {
            throw ite.targetException
        }
    }

    private suspend fun onDeactivate() {
        logger.debug { "Deactivating $reference..." }
        stopwatch(clock) {
            if (implDefinition.lifecycle.autoDeactivate) {
                implDefinition.onDeactivateMethod?.also {
                    DeferredWrappers.wrapCall(it.method.invoke(instance)).await()
                }
            }

            worker.cancel()
            channel.close()
            drainChannel()

        }.also { (elapsed, _) ->
            logger.debug { "Deactivated $reference in ${elapsed}ms." }
        }
    }

    private suspend fun drainChannel() {
        for (event in channel) {
            if (event is EventType.InvokeEvent) {
                logger.warn(
                    "Received invocation which can no longer be handled locally. " +
                            "Rerouting... ${event.invocation}"
                )

                pipeline.pushInvocation(event.invocation)
            }
        }
    }

    private val worker = runtimeScopes.cpuScope.launch {
        for (event in channel) {
            try {
                when (event) {
                    is EventType.ActivateEvent -> onActivate()
                    is EventType.InvokeEvent -> onInvoke(event.invocation)
                    is EventType.DeactivateEvent -> onDeactivate()
                }.also {
                    event.completion.complete(it)
                }
            } catch (t: Throwable) {
                event.completion.completeExceptionally(t)
            }
        }
    }

    private sealed class EventType {
        abstract val completion: Completion

        data class ActivateEvent(override val completion: Completion) : EventType()
        data class InvokeEvent(val invocation: AddressableInvocation, override val completion: Completion) : EventType()
        data class DeactivateEvent(override val completion: Completion) : EventType()
    }
}