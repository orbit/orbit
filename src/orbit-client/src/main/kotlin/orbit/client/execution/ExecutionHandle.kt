/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.client.OrbitClient
import orbit.client.addressable.AbstractAddressable
import orbit.client.addressable.Addressable
import orbit.client.addressable.AddressableContext
import orbit.client.addressable.AddressableImplDefinition
import orbit.client.addressable.DeactivationReason
import orbit.client.addressable.InvocationSystem
import orbit.client.addressable.MethodInvoker
import orbit.client.net.Completion
import orbit.client.util.DeferredWrappers
import orbit.shared.addressable.AddressableInvocation
import orbit.shared.addressable.AddressableReference
import orbit.shared.exception.CapacityExceededException
import orbit.shared.net.InvocationReason
import orbit.util.concurrent.SupervisorScope
import orbit.util.di.ComponentContainer
import orbit.util.time.Clock
import orbit.util.time.stopwatch
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.jvm.kotlinFunction

internal class ExecutionHandle(
    val instance: Addressable,
    override val reference: AddressableReference,
    val implDefinition: AddressableImplDefinition,
    componentContainer: ComponentContainer
) : Deactivatable {
    private val orbitClient: OrbitClient by componentContainer.inject()
    private val clock: Clock by componentContainer.inject()
    private val supervisorScope: SupervisorScope by componentContainer.inject()
    private val invocationSystem: InvocationSystem by componentContainer.inject()
    private val addressableBufferCount = 128

    private val logger = KotlinLogging.logger { }

    val createdTime = clock.currentTime

    @Volatile
    var deactivateNextTick = false

    private val lastActivityAtomic = AtomicReference(createdTime)
    val lastActivity get() = lastActivityAtomic.get()
    var active: Boolean = false

    private val channel = Channel<EventType>(addressableBufferCount)

    init {
        if (instance is AbstractAddressable) {
            instance.context = AddressableContext(
                reference = reference,
                client = orbitClient
            )
        }
    }

    fun activate(): Completion =
        CompletableDeferred<Any?>().also {
            sendEvent(EventType.ActivateEvent(it))
        }

    override fun deactivate(deactivationReason: DeactivationReason): Completion =
        CompletableDeferred<Any?>().also {
            sendEvent(EventType.DeactivateEvent(deactivationReason, it))
        }

    suspend fun invoke(
        invocation: AddressableInvocation
    ): Completion =
        CompletableDeferred<Any?>().also {
            sendEvent(EventType.InvokeEvent(invocation, it))
        }


    private fun sendEvent(eventType: EventType) {
        if (!channel.offer(eventType)) {
            val errMsg = "Buffer capacity exceeded (>${addressableBufferCount}) for $reference"
            logger.error(errMsg)
            throw CapacityExceededException(errMsg)
        }
    }

    private suspend fun onActivate() = coroutineScope {
        logger.debug { "Activating $reference..." }
        stopwatch(clock) {
            implDefinition.onActivateMethod?.also {
                if (it.method.kotlinFunction?.isSuspend == true) {
                    DeferredWrappers.wrapSuspend(it.method, instance)
                } else {
                    DeferredWrappers.wrapCall(it.method.invoke(instance)).await()
                }
            }
        }.also { (elapsed, _) ->
            logger.debug { "Activated $reference in ${elapsed}ms. " }
            active = true
        }
    }

    private suspend fun onInvoke(invocation: AddressableInvocation): Any? {
        lastActivityAtomic.set(clock.currentTime)

        try {
            return MethodInvoker.invoke(instance, invocation.method, invocation.args)
        } catch (ite: InvocationTargetException) {
            throw ite.targetException
        }
    }

    private suspend fun onDeactivate(deactivationReason: DeactivationReason) {
        logger.debug { "Deactivating $reference..." }
        stopwatch(clock) {
            implDefinition.onDeactivateMethod?.also {
                val isSuspended = (it.method.kotlinFunction?.isSuspend == true)
                val hasReason = it.method.parameterTypes.firstOrNull() == DeactivationReason::class.java

                val reasonArgs: Array<DeactivationReason> = if (hasReason) arrayOf(deactivationReason) else emptyArray()
                // TODO (brett) - Check has 0..1 reason parameter without suspended
                //  logger.warn { "Methods with a single argument tagged as @OnDeactivate may only accept a DeactivationReason" }
                //  logger.warn { "Methods tagged with @OnDeactivate may only take 0 or 1 argument(s)" }


                if (isSuspended) {
                    DeferredWrappers.wrapSuspend(
                        it.method,
                        instance,
                        reasonArgs
                    )
                } else {
                    DeferredWrappers.wrapCall(it.method.invoke(instance, *reasonArgs)).await()
                }
            }

            worker.cancel()
            channel.close()
            drainChannel()

        }.also { (elapsed, _) ->
            logger.debug { "Deactivated $reference in ${elapsed}ms." }
            active = false
        }
    }

    private suspend fun drainChannel() {
        for (event in channel) {
            if (event is EventType.InvokeEvent) {
                logger.warn(
                    "Received invocation which can no longer be handled locally. Rerouting... ${event.invocation}"
                )

                invocationSystem.sendInvocation(
                    event.invocation.copy(
                        reason = InvocationReason.rerouted
                    )
                )
            }
        }
    }

    private val worker = supervisorScope.launch {
        for (event in channel) {
            try {
                when (event) {
                    is EventType.ActivateEvent -> onActivate()
                    is EventType.InvokeEvent -> onInvoke(event.invocation)
                    is EventType.DeactivateEvent -> onDeactivate(event.deactivationReason)
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
        data class DeactivateEvent(val deactivationReason: DeactivationReason, override val completion: Completion) :
            EventType()
    }
}