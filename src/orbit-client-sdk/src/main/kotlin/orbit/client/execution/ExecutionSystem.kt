/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import orbit.client.addressable.Addressable
import orbit.client.addressable.AddressableClass
import orbit.client.addressable.AddressableDefinitionDirectory
import orbit.client.addressable.AddressableImplDefinition
import orbit.client.net.Completion
import orbit.shared.addressable.AddressableInvocation
import orbit.shared.addressable.AddressableReference
import orbit.util.di.jvm.ComponentContainer
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionSystem(
    private val executionLeases: ExecutionLeases,
    private val definitionDirectory: AddressableDefinitionDirectory,
    private val componentContainer: ComponentContainer
) {
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ExecutionHandle>()

    suspend fun handleInvocation(invocation: AddressableInvocation, completion: Completion) {
        executionLeases.getOrRenewLease(invocation.reference)

        var handle = activeAddressables[invocation.reference]

        if (handle == null) {
            handle = activate(invocation.reference)
        }

        checkNotNull(handle) { "No active addressable found for ${invocation.reference}" }

        // Call
        invoke(handle, invocation, completion)
    }

    private suspend fun activate(
        reference: AddressableReference
    ): ExecutionHandle? =
        definitionDirectory.getImplDefinition(reference.type).let {
            val handle = getOrCreateAddressable(reference, it)
            handle.activate().await()
            handle
        }

    private suspend fun invoke(handle: ExecutionHandle, invocation: AddressableInvocation, completion: Completion) {
        val result = handle.invoke(invocation).await()
        completion.complete(result)
    }

    private suspend fun getOrCreateAddressable(
        reference: AddressableReference,
        implDefinition: AddressableImplDefinition
    ): ExecutionHandle =
        activeAddressables.getOrPut(reference) {
            val newInstance = createInstance(implDefinition.implClass)
            createHandle(reference, implDefinition, newInstance)
        }.also {
            //registerHandle(it)
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

    private fun createInstance(addressableClass: AddressableClass): Addressable {
        return addressableClass.getDeclaredConstructor().newInstance()
    }

}