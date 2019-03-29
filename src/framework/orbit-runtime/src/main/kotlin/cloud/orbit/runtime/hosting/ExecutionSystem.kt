/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.logging.debug
import cloud.orbit.common.logging.logger
import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.stopwatch
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.pipeline.PipelineSystem
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinitionDictionary
import cloud.orbit.runtime.stage.StageConfig
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionSystem(
    private val componentProvider: ComponentProvider,
    private val capabilitiesScanner: CapabilitiesScanner,
    private val interfaceDefinitionDictionary: AddressableInterfaceDefinitionDictionary,
    private val clock: Clock,
    private val stageConfig: StageConfig,
    private val directorySystem: DirectorySystem,
    private val routingSystem: RoutingSystem,
    private val pipelineSystem: PipelineSystem
) {
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ExecutionHandle>()
    private val logger by logger()

    suspend fun handleInvocation(invocation: AddressableInvocation, completion: Completion) {
        val definition =
            interfaceDefinitionDictionary.getOrCreate(invocation.reference.interfaceClass)
        var handle = activeAddressables[invocation.reference]

        if(handle == null) {
            if(routingSystem.canHandleLocally(invocation.reference)) {
                if(definition.lifecycle.autoActivate) {
                    handle = activate(invocation.reference, definition)
                }
            } else {
                logger.warn("Received invocation which can no longer be handled locally. Rerouting... $invocation")
                pipelineSystem.pushInvocation(invocation)
            }
        }

        if (handle == null) {
            throw IllegalStateException("No active addressable found for $definition")
        }

        // Call
        invoke(handle, invocation, completion)
    }

    suspend fun onTick() {
        val tickTime = clock.currentTime
        activeAddressables.forEach { (_, handle) ->
            if (handle.definition.lifecycle.autoDeactivate) {
                if (tickTime - handle.lastActivity > stageConfig.timeToLiveMillis) {
                    deactivate(handle)
                }
            }
        }
    }

    private suspend fun activate(
        reference: AddressableReference,
        definition: AddressableInterfaceDefinition
    ): ExecutionHandle {
        logger.debug { "Activating $reference..."}
        val (elapsed, handle) = stopwatch(clock) {
            val handle = getOrCreateAddressable(reference, definition)
            if (handle.definition.routing.persistentPlacement) {
                directorySystem.localActivation(handle.reference)
            }
            handle.activate().await()
            handle
        }
        logger.debug { "Activated $reference in ${elapsed}ms. " }
        return handle
    }

    private suspend fun invoke(handle: ExecutionHandle, invocation: AddressableInvocation, completion: Completion) {
        handle.invoke(invocation, completion)
    }

    private suspend fun deactivate(handle: ExecutionHandle) {
        logger.debug { "Deactivating ${handle.reference}..." }
        val (elapsed, _) = stopwatch(clock) {
            handle.deactivate().await()
            activeAddressables.remove(handle.reference)
            if (handle.definition.routing.persistentPlacement) {
                directorySystem.localDeactivation(handle.reference)
            }
        }
        logger.debug { "Deactivated ${handle.reference} in ${elapsed}ms." }
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

