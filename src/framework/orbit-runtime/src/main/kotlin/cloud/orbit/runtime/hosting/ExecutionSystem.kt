/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.time.Clock
import cloud.orbit.core.remoting.ActivatedAddressable
import cloud.orbit.core.remoting.ActiveAddressable
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinitionDictionary
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import java.util.concurrent.ConcurrentHashMap

class ExecutionSystem(
    private val clock: Clock,
    private val capabilitiesScanner: CapabilitiesScanner,
    private val interfaceDefinitionDictionary: AddressableInterfaceDefinitionDictionary
) {
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ActiveAddressable>()


    suspend fun handleInvocation(addressableInvocation: AddressableInvocation, completion: Completion) {
        val addressableInterfaceDefinition =
            interfaceDefinitionDictionary.getOrCreate(addressableInvocation.reference.interfaceClass)
        var active = activeAddressables[addressableInvocation.reference]

        // Activation
        if (active == null && addressableInterfaceDefinition.lifecycle.autoActivate) {
            active = getOrCreateAddressable(addressableInvocation.reference)
        }
        if (active == null) {
            throw IllegalStateException("No active addressable found for $addressableInterfaceDefinition")
        }
        val instance = active.instance
        if(instance is ActivatedAddressable) {
            instance.context = ActivatedAddressable.AddressableContext(
                reference = active.addressableReference

            )
        }

        // Call
        dispatchInvocation(addressableInvocation, completion, active.instance)

        // Update timestamp
        activeAddressables.replace(addressableInvocation.reference, active, active.copy(lastActivity = clock.currentTime))
    }

    private suspend fun dispatchInvocation(
        addressableInvocation: AddressableInvocation,
        completion: Completion,
        addressable: Addressable
    ) {
        val rawResult = addressableInvocation.method.invoke(addressable, *addressableInvocation.args)
        try {
            val result = DeferredWrappers.wrapCall(rawResult).await()
            completion.complete(result)
        } catch (t: Throwable) {
            completion.completeExceptionally(t)
        }
    }

    private fun getOrCreateAddressable(addressableReference: AddressableReference) = activeAddressables.getOrPut(addressableReference) {
        val newInstance = createInstance(addressableReference)
        ActiveAddressable(
            instance = newInstance,
            lastActivity = clock.currentTime,
            addressableReference = addressableReference
        )
    }

    private fun createInstance(addressableReference: AddressableReference): Addressable {
        val newInstanceType = capabilitiesScanner.interfaceLookup.getValue(addressableReference.interfaceClass)
        return newInstanceType.getDeclaredConstructor().newInstance()
    }
}