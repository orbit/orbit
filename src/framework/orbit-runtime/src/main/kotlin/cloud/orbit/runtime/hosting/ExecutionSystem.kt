/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.TimeMs
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.RemoteInterfaceDefinitionDictionary
import cloud.orbit.runtime.remoting.RemoteInvocation
import cloud.orbit.runtime.remoting.RemoteInvocationTarget
import java.util.concurrent.ConcurrentHashMap

class ExecutionSystem(
    private val clock: Clock,
    private val capabilitiesScanner: CapabilitiesScanner,
    private val remoteInterfaceDefinitionDictionary: RemoteInterfaceDefinitionDictionary
) {
    data class ActiveAddressable(
        val lastActivity: TimeMs,
        val instance: Addressable
    )

    private val activeAddressables = ConcurrentHashMap<RemoteInvocationTarget, ActiveAddressable>()


    suspend fun handleInvocation(remoteInvocation: RemoteInvocation, completion: Completion) {
        val rid = remoteInterfaceDefinitionDictionary.getOrCreate(remoteInvocation.target.interfaceClass)
        var active = activeAddressables[remoteInvocation.target]

        // Activation
        if (active == null) {
            if (rid.lifecycle.autoActivate) {
                active = getOrCreateAddressable(remoteInvocation.target)
            }
        }
        if (active == null) {
            throw IllegalArgumentException("No active addressable found for $rid")
        }

        // Call
        dispatchInvocation(remoteInvocation, completion, active.instance)
    }

    private suspend fun dispatchInvocation(
        remoteInvocation: RemoteInvocation,
        completion: Completion,
        addressable: Addressable
    ) {
        val rawResult = remoteInvocation.method.invoke(addressable, *remoteInvocation.args)
        try {
            val result = DeferredWrappers.wrapCall(rawResult).await()
            completion.complete(result)
        } catch (t: Throwable) {
            completion.completeExceptionally(t)
        }
    }

    private fun getOrCreateAddressable(rit: RemoteInvocationTarget) = activeAddressables.getOrPut(rit) {
        val newInstance = createInstance(rit)
        ActiveAddressable(
            instance = newInstance,
            lastActivity = clock.currentTime
        )
    }

    private fun createInstance(rit: RemoteInvocationTarget): Addressable {
        val newInstanceType = capabilitiesScanner.interfaceLookup.getValue(rit.interfaceClass)
        return newInstanceType.getDeclaredConstructor().newInstance()
    }
}