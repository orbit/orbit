/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.time.Clock
import cloud.orbit.core.remoting.ActivatedAddressable
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicLong

internal class ExecutionHandle(
    val instance: Addressable,
    val reference: AddressableReference,
    val definition: AddressableInterfaceDefinition,
    componentProvider: ComponentProvider
) {
    private val clock: Clock by componentProvider.inject()
    private val supervisorScope: SupervisorScope by componentProvider.inject()

    val createdTime = clock.currentTime

    private val lastActivityAtomic = AtomicLong(createdTime)
    val lastActivity get() = lastActivityAtomic.get()

    private val channel = Channel<EventType>(UNLIMITED)

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

    suspend fun activate(): Completion {
        val completion = CompletableDeferred<Any?>()
        channel.send(EventType.ActivateEvent(completion))
        return completion
    }

    suspend fun deactivate(): Completion {
        val completion = CompletableDeferred<Any?>()
        channel.send(EventType.DeactivateEvent(completion))
        return completion
    }

    suspend fun invoke(
        invocation: AddressableInvocation,
        completion: Completion
    ) : Completion{
        channel.send(EventType.InvokeEvent(invocation, completion))
        return completion
    }

    private fun onActivate() {
        if (instance is ActivatedAddressable) {
            instance.context = ActivatedAddressable.AddressableContext(
                reference = reference
            )
        }
    }

    private suspend fun onInvoke(invocation: AddressableInvocation): Any? {
        lastActivityAtomic.set(clock.currentTime)
        try {
            val rawResult = invocation.method.invoke(instance, *invocation.args)
            return DeferredWrappers.wrapCall(rawResult).await()
        } catch(ite: InvocationTargetException) {
            throw ite.targetException
        } catch(t: Throwable) {
            throw t
        }
    }

    private fun onDeactivate() {
        worker.cancel()
    }

    private sealed class EventType {
        abstract val completion: Completion

        data class ActivateEvent(override val completion: Completion) : EventType()
        data class InvokeEvent(val invocation: AddressableInvocation, override val completion: Completion) : EventType()
        data class DeactivateEvent(override val completion: Completion) : EventType()
    }
}