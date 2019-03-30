/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.logging.logger
import cloud.orbit.common.time.Clock
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableClass
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.pipeline.PipelineSystem
import cloud.orbit.runtime.remoting.AddressableDefinitionDirectory
import cloud.orbit.runtime.remoting.AddressableImplDefinition
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import cloud.orbit.runtime.stage.StageConfig
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionSystem(
    private val componentProvider: ComponentProvider,
    private val definitionDirectory: AddressableDefinitionDirectory,
    private val clock: Clock,
    private val stageConfig: StageConfig,
    private val directorySystem: DirectorySystem,
    private val routingSystem: RoutingSystem,
    private val pipelineSystem: PipelineSystem
) {
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ExecutionHandle>()
    private val logger by logger()

    suspend fun handleInvocation(invocation: AddressableInvocation, completion: Completion) {
        val interfaceDefinition =
            definitionDirectory.getOrCreateInterfaceDefinition(invocation.reference.interfaceClass)
        var handle = activeAddressables[invocation.reference]

        if (handle == null) {
            if (routingSystem.canHandleLocally(invocation.reference)) {
                if (interfaceDefinition.lifecycle.autoActivate) {
                    handle = activate(invocation.reference, interfaceDefinition)
                }
            } else {
                logger.warn("Received invocation which can no longer be handled locally. Rerouting... $invocation")
                pipelineSystem.pushInvocation(invocation)
            }
        }

        if (handle == null) {
            throw IllegalStateException("No active addressable found for $interfaceDefinition")
        }

        // Call
        invoke(handle, invocation, completion)
    }

    suspend fun onTick() {
        val tickTime = clock.currentTime
        activeAddressables.forEach { (_, handle) ->
            if (handle.interfaceDefinition.lifecycle.autoDeactivate) {
                if (tickTime - handle.lastActivity > stageConfig.timeToLiveMillis) {
                    deactivate(handle)
                }
            }
        }
    }

    suspend fun onStop() {
        activeAddressables.forEach { (_, handle) ->
            deactivate(handle)
        }
    }

    private suspend fun activate(
        reference: AddressableReference,
        interfaceDefinition: AddressableInterfaceDefinition
    ): ExecutionHandle {
        val implDefinition = definitionDirectory.getImplDefinition(interfaceDefinition.interfaceClass)
        val handle = getOrCreateAddressable(reference, implDefinition)
        if (handle.interfaceDefinition.routing.persistentPlacement) {
            directorySystem.localActivation(handle.reference)
        }
        handle.activate().await()
        return handle
    }

    private fun invoke(handle: ExecutionHandle, invocation: AddressableInvocation, completion: Completion) {
        handle.invoke(invocation, completion)
    }

    private suspend fun deactivate(handle: ExecutionHandle) {
        handle.deactivate().await()
        activeAddressables.remove(handle.reference)
        if (handle.interfaceDefinition.routing.persistentPlacement) {
            directorySystem.localDeactivation(handle.reference)
        }
    }

    private fun getOrCreateAddressable(
        reference: AddressableReference,
        implDefinition: AddressableImplDefinition
    ): ExecutionHandle =
        activeAddressables.getOrPut(reference) {
            val newInstance = createInstance(implDefinition.implClass)
            ExecutionHandle(
                componentProvider = componentProvider,
                instance = newInstance,
                reference = reference,
                interfaceDefinition = implDefinition.interfaceDefinition,
                implDefinition = implDefinition
            )
        }

    private fun createInstance(addressableClass: AddressableClass): Addressable {
        return addressableClass.getDeclaredConstructor().newInstance()
    }
}

