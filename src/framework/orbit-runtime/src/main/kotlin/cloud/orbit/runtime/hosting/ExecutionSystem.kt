/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.time.Clock
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinitionDictionary
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionSystem(
    private val componentProvider: ComponentProvider,
    private val capabilitiesScanner: CapabilitiesScanner,
    private val interfaceDefinitionDictionary: AddressableInterfaceDefinitionDictionary,
    private val clock: Clock,
    private val stageConfig: StageConfig,
    private val directorySystem: DirectorySystem
) {
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ExecutionHandle>()

    suspend fun handleInvocation(invocation: AddressableInvocation, completion: Completion) {
        val definition =
            interfaceDefinitionDictionary.getOrCreate(invocation.reference.interfaceClass)
        var handler = activeAddressables[invocation.reference]

        // Activation
        if (handler == null && definition.lifecycle.autoActivate) {
            handler = activate(invocation.reference, definition)
        }
        if (handler == null) {
            throw IllegalStateException("No active addressable found for $definition")
        }

        // Call
        invoke(handler, invocation, completion)
    }

    suspend fun onTick() {
        val tickTime = clock.currentTime
        activeAddressables.forEach { (_, handler) ->
            if (handler.definition.lifecycle.autoDeactivate) {
                if (tickTime - handler.lastActivity > stageConfig.timeToLiveMillis) {
                    deactivate(handler)
                }
            }
        }
    }

    private suspend fun activate(
        reference: AddressableReference,
        definition: AddressableInterfaceDefinition
    ): ExecutionHandle {
        val handle = getOrCreateAddressable(reference, definition)
        if (handle.definition.routing.persistentPlacement) {
            directorySystem.localActivation(handle.reference)
        }
        handle.activate().await()
        return handle
    }

    private suspend fun invoke(handle: ExecutionHandle, invocation: AddressableInvocation, completion: Completion) {
        handle.invoke(invocation, completion)
    }

    private suspend fun deactivate(handle: ExecutionHandle) {
        handle.deactivate().await()
        if (handle.definition.routing.persistentPlacement) {
            directorySystem.localDeactivation(handle.reference)
        }
        activeAddressables.remove(handle.reference)
    }

    private fun getOrCreateAddressable(
        reference: AddressableReference,
        definition: AddressableInterfaceDefinition
    ): ExecutionHandle =
        activeAddressables.getOrPut(reference) {
            val newInstance = createInstance(reference)
            ExecutionHandle(
                componentProvider = componentProvider,
                instance = newInstance,
                reference = reference,
                definition = definition
            )
        }

    private fun createInstance(reference: AddressableReference): Addressable {
        val newInstanceType = capabilitiesScanner.interfaceLookup.getValue(reference.interfaceClass)
        return newInstanceType.getDeclaredConstructor().newInstance()
    }
}

