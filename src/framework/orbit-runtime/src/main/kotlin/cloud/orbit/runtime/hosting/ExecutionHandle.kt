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
import cloud.orbit.core.remoting.ActivatedAddressable
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Completion
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
    private val supervisorScope: SupervisorScope by componentProvider.inject()
    private val stageConfig: StageConfig by componentProvider.inject()

    private val logger by logger()

    val createdTime = clock.currentTime

    private val lastActivityAtomic = AtomicLong(createdTime)
    val lastActivity get() = lastActivityAtomic.get()

    private val channel = Channel<EventType>(stageConfig.addressableBufferCount)

    fun activate(): Completion {
        val completion = CompletableDeferred<Any?>()
        sendEvent(EventType.ActivateEvent(completion))
        return completion
    }

    fun deactivate(): Completion {
        val completion = CompletableDeferred<Any?>()
        sendEvent(EventType.DeactivateEvent(completion))
        return completion
    }

    fun invoke(
        invocation: AddressableInvocation,
        completion: Completion
    ): Completion {
        sendEvent(EventType.InvokeEvent(invocation, completion))
        return completion
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
        val (elapsed, _) = stopwatch(clock) {
            if (instance is ActivatedAddressable) {
                instance.context = ActivatedAddressable.AddressableContext(
                    reference = reference
                )
            }

            implDefinition.onActivateMethod?.also {
                DeferredWrappers.wrapCall(it.method.invoke(instance)).await()
            }
        }
        logger.debug { "Activated $reference in ${elapsed}ms. " }
    }

    private suspend fun onInvoke(invocation: AddressableInvocation): Any? {
        lastActivityAtomic.set(clock.currentTime)
        try {
            val rawResult = invocation.method.invoke(instance, *invocation.args)
            return DeferredWrappers.wrapCall(rawResult).await()
        } catch (ite: InvocationTargetException) {
            throw ite.targetException
        } catch (t: Throwable) {
            throw t
        }
    }

    private suspend fun onDeactivate() {
        logger.debug { "Deactivating $reference..." }
        val (elapsed, _) = stopwatch(clock) {
            implDefinition.onDeactivateMethod?.also {
                DeferredWrappers.wrapCall(it.method.invoke(instance)).await()
            }

            worker.cancel()
        }
        logger.debug { "Deactivated $reference in ${elapsed}ms." }
    }

    private val worker = supervisorScope.launch {
        for (event in channel) {
            try {
                val result = when (event) {
                    is EventType.ActivateEvent -> onActivate()
                    is EventType.InvokeEvent -> onInvoke(event.invocation)
                    is EventType.DeactivateEvent -> onDeactivate()
                }
                event.completion.complete(result)
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