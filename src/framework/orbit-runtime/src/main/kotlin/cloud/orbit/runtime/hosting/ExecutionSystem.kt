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
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinitionDictionary
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ExecutionSystem(
    private val clock: Clock,
    private val capabilitiesScanner: CapabilitiesScanner,
    private val interfaceDefinitionDictionary: AddressableInterfaceDefinitionDictionary
) {
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ExeuctionHandler>()

    suspend fun handleInvocation(invocation: AddressableInvocation, completion: Completion) {
        val addressableInterfaceDefinition =
            interfaceDefinitionDictionary.getOrCreate(invocation.reference.interfaceClass)
        var handler = activeAddressables[invocation.reference]

        // Activation
        if (handler == null && addressableInterfaceDefinition.lifecycle.autoActivate) {
            handler = getOrCreateAddressable(invocation.reference)
        }
        if (handler == null) {
            throw IllegalStateException("No active addressable found for $addressableInterfaceDefinition")
        }
        val instance = handler.instance
        if (instance is ActivatedAddressable) {
            instance.context = ActivatedAddressable.AddressableContext(
                reference = handler.reference

            )
        }

        // Call
        handler.dispatchInvocation(invocation, completion)
    }

    private fun getOrCreateAddressable(reference: AddressableReference) =
        activeAddressables.getOrPut(reference) {
            val newInstance = createInstance(reference)
            ExeuctionHandler(
                instance = newInstance,
                reference = reference
            )
        }

    private fun createInstance(reference: AddressableReference): Addressable {
        val newInstanceType = capabilitiesScanner.interfaceLookup.getValue(reference.interfaceClass)
        return newInstanceType.getDeclaredConstructor().newInstance()
    }

    private inner class ExeuctionHandler(
        val instance: Addressable,
        val reference: AddressableReference
    ) {
        val createdTime = clock.currentTime
        val lastActivity get() = lastActivityAtomic.get()

        private val lastActivityAtomic = AtomicLong(createdTime)

        suspend fun dispatchInvocation(
            invocation: AddressableInvocation,
            completion: Completion
        ) {
            lastActivityAtomic.set(clock.currentTime)
            val rawResult = invocation.method.invoke(instance, *invocation.args)
            try {
                val result = DeferredWrappers.wrapCall(rawResult).await()
                completion.complete(result)
            } catch (t: Throwable) {
                completion.completeExceptionally(t)
            }
        }
    }
}

