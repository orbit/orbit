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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionSystem(
    private val componentProvider: ComponentProvider,
    private val definitionDirectory: AddressableDefinitionDirectory,
    private val clock: Clock,
    private val stageConfig: StageConfig,
    private val directorySystem: DirectorySystem
) {
    private val activeAddressables = ConcurrentHashMap<AddressableReference, ExecutionHandle>()
    private val instanceAddressableMap = ConcurrentHashMap<Addressable, ExecutionHandle>()

    private val logger by logger()

    suspend fun handleInvocation(invocation: AddressableInvocation, completion: Completion) {
        val interfaceDefinition =
            definitionDirectory.getOrCreateInterfaceDefinition(invocation.reference.interfaceClass)
        var handle = activeAddressables[invocation.reference]

        if (handle == null) {
            handle = activate(invocation.reference, interfaceDefinition)
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
            val ttlDeactivate = handle.implDefinition.lifecycle.autoDeactivate &&
                    (tickTime - handle.lastActivity > stageConfig.timeToLiveMillis)

            if (ttlDeactivate || handle.deactivateNextTick) {
                deactivate(handle)
            }
        }
    }

    suspend fun onStop() {
        activeAddressables.forEach { (_, handle) ->
            deactivate(handle)
        }
    }

    suspend fun registerAddressableInstance(reference: AddressableReference, addressable: Addressable) {
        val handle = createHandle(
            reference, definitionDirectory.onDemandImplClass(
                reference.interfaceClass,
                addressable.javaClass
            ), addressable
        )
        registerHandle(handle)
    }

    suspend fun deregisterAddressableInstance(addressable: Addressable) {
        instanceAddressableMap[addressable]?.also {
            deactivate(it)
        }
    }

    fun getReferenceByInstance(addressable: Addressable): AddressableReference? =
        instanceAddressableMap[addressable]?.reference

    private suspend fun activate(
        reference: AddressableReference,
        interfaceDefinition: AddressableInterfaceDefinition
    ): ExecutionHandle? {
        val implDefinition = definitionDirectory.getImplDefinition(interfaceDefinition.interfaceClass)
        if (implDefinition.lifecycle.autoActivate) {
            val handle = getOrCreateAddressable(reference, implDefinition)
            handle.activate().await()
            return handle
        }
        return null
    }

    private fun invoke(handle: ExecutionHandle, invocation: AddressableInvocation, completion: Completion) {
        handle.invoke(invocation, completion)
    }

    private suspend fun deactivate(handle: ExecutionHandle) {
        try {
            withTimeout(stageConfig.deactivationTimeoutMillis) {
                handle.deactivate().await()
            }
        } catch (t: TimeoutCancellationException) {
            val msg = "A timeout occurred (>${stageConfig.deactivationTimeoutMillis}ms) during deactivation of " +
                    "${handle.reference}. This addressable is now considered deactivated, this may cause state " +
                    "corruption."
            logger.error(msg)
        }
        deregisterHandle(handle)
    }

    private suspend fun getOrCreateAddressable(
        reference: AddressableReference,
        implDefinition: AddressableImplDefinition
    ): ExecutionHandle {
        val handle = activeAddressables.getOrPut(reference) {
            val newInstance = createInstance(implDefinition.implClass)
            createHandle(reference, implDefinition, newInstance)
        }
        registerHandle(handle)
        return handle
    }

    private fun createHandle(
        reference: AddressableReference,
        implDefinition: AddressableImplDefinition,
        instance: Addressable
    ): ExecutionHandle =
        ExecutionHandle(
            componentProvider = componentProvider,
            instance = instance,
            reference = reference,
            interfaceDefinition = implDefinition.interfaceDefinition,
            implDefinition = implDefinition
        )

    private suspend fun registerHandle(handle: ExecutionHandle) {
        activeAddressables[handle.reference] = handle
        instanceAddressableMap[handle.instance] = handle
        updatePlacement(handle, true)
    }

    private suspend fun deregisterHandle(handle: ExecutionHandle) {
        activeAddressables.remove(handle.reference)
        instanceAddressableMap.remove(handle.instance)
        updatePlacement(handle, false)
    }

    private suspend fun updatePlacement(handle: ExecutionHandle, adding: Boolean) {
        if (handle.interfaceDefinition.routing.persistentPlacement) {
            if (adding) {
                directorySystem.forcePlaceLocal(handle.reference)
            } else {
                directorySystem.removeIfLocal(handle.reference)
            }
        }
    }


    private fun createInstance(addressableClass: AddressableClass): Addressable {
        return addressableClass.getDeclaredConstructor().newInstance()
    }
}

