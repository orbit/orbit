/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import orbit.client.OrbitClientConfig
import orbit.client.addressable.Addressable
import orbit.client.addressable.AddressableClass
import orbit.client.addressable.AddressableConstructor
import orbit.client.addressable.AddressableDefinitionDirectory
import orbit.client.addressable.AddressableImplDefinition
import orbit.client.addressable.DeactivationReason
import orbit.client.net.Completion
import orbit.shared.addressable.AddressableInvocation
import orbit.shared.addressable.AddressableReference
import orbit.util.di.ComponentContainer
import orbit.util.time.Clock
import orbit.util.time.Timestamp
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionSystem(
    private val executionLeases: ExecutionLeases,
    private val definitionDirectory: AddressableDefinitionDirectory,
    private val componentContainer: ComponentContainer,
    private val clock: Clock,
    private val addressableConstructor: AddressableConstructor,
    config: OrbitClientConfig
) {
    private val logger = KotlinLogging.logger { }
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ExecutionHandle>()
    private val deactivationTimeoutMs = config.deactivationTimeout.toMillis()
    private val defaultTtl = config.addressableTTL.toMillis()

    suspend fun handleInvocation(invocation: AddressableInvocation, completion: Completion) {
        try {
            executionLeases.getOrRenewLease(invocation.reference)

            var handle = activeAddressables[invocation.reference]

            if (handle == null) {
                handle = activate(invocation.reference)
            }

            checkNotNull(handle) { "No active addressable found for ${invocation.reference}" }

            // Call
            val result = handle.invoke(invocation).await()
            completion.complete(result)

        } catch (t: Throwable) {
            completion.completeExceptionally(t)
        }
    }

    suspend fun tick() {
        activeAddressables.forEach { (_, handle) ->
            val timeInactive = clock.currentTime - handle.lastActivity


            if (handle.deactivateNextTick) {
                deactivate(handle, DeactivationReason.EXTERNALLY_TRIGGERED)
                return@forEach
            }

            if (timeInactive > defaultTtl) {
                deactivate(handle, DeactivationReason.TTL_EXPIRED)
                return@forEach
            }

            val lease = executionLeases.getLease(handle.reference)
            if (lease != null) {
                if (Timestamp.now() > lease.renewAt) {
                    try {
                        executionLeases.renewLease(handle.reference)
                    } catch (t: Throwable) {
                        logger.error(t) { "Unexpected error renewing lease" }
                        deactivate(handle, DeactivationReason.LEASE_RENEWAL_FAILED)
                        return@forEach
                    }
                }
            } else {
                logger.error { "No lease found for ${handle.reference}" }
                deactivate(handle, DeactivationReason.LEASE_RENEWAL_FAILED)
                return@forEach
            }
        }
    }

    suspend fun stop() {
        activeAddressables.forEach {
            deactivate(it.value, DeactivationReason.NODE_SHUTTING_DOWN)
        }
    }

    private suspend fun activate(
        reference: AddressableReference
    ): ExecutionHandle? =
        definitionDirectory.getImplDefinition(reference.type).let {
            val handle = getOrCreateAddressable(reference, it)
            handle.activate().await()
            handle
        }

    private suspend fun deactivate(handle: ExecutionHandle, deactivationReason: DeactivationReason) {
        try {
            withTimeout(deactivationTimeoutMs) {
                handle.deactivate(deactivationReason).await()
            }
        } catch (t: TimeoutCancellationException) {
            val msg = "A timeout occurred (>${deactivationTimeoutMs}ms) during deactivation of " +
                    "${handle.reference}. This addressable is now considered deactivated, this may cause state " +
                    "corruption."
            logger.error(msg)
        }
        executionLeases.abandonLease(handle.reference)
        activeAddressables.remove(handle.reference)
    }

    private suspend fun getOrCreateAddressable(
        reference: AddressableReference,
        implDefinition: AddressableImplDefinition
    ): ExecutionHandle =
        activeAddressables.getOrPut(reference) {
            val newInstance = createInstance(implDefinition.implClass)
            createHandle(reference, implDefinition, newInstance)
        }


    private fun createHandle(
        reference: AddressableReference,
        implDefinition: AddressableImplDefinition,
        instance: Addressable
    ): ExecutionHandle =
        ExecutionHandle(
            componentContainer = componentContainer,
            instance = instance,
            reference = reference,
            interfaceDefinition = implDefinition.interfaceDefinition,
            implDefinition = implDefinition
        )

    private fun createInstance(addressableClass: AddressableClass): Addressable =
        addressableConstructor.constructAddressable(addressableClass)
}