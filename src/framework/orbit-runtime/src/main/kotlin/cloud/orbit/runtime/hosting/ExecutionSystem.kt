/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.TimeMs
import cloud.orbit.runtime.capabilities.CapabilitiesScanner
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.RemoteInvocation
import cloud.orbit.runtime.remoting.RemoteInvocationTarget
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ExecutionSystem(
    private val clock: Clock,
    private val capabilitiesScanner: CapabilitiesScanner
) {
    data class ActiveAddressable(
        val lastActivity: TimeMs,
        val instance: Any
    )

    private val activeAddressables = ConcurrentHashMap<RemoteInvocationTarget, AtomicReference<ActiveAddressable>>()

    suspend fun handleInvocation(remoteInvocation: RemoteInvocation, completion: Completion) {
        // Activate
        val active = getOrCreateAddressable(remoteInvocation.target)?.get()
            ?: throw IllegalArgumentException(
                "Active Addressable  does not exist and can not be created. ${remoteInvocation.target}"
            )

        // Call
        val rawResult = remoteInvocation.methodDefinition.method.invoke(active.instance, *remoteInvocation.args)
        try {
            val result = DeferredWrappers.wrapCall(rawResult).await()
            completion.complete(result)
        } catch (t: Throwable) {
            completion.completeExceptionally(t)
        }
    }


    private fun getOrCreateAddressable(rit: RemoteInvocationTarget) = activeAddressables.getOrPut(rit) {
        if (shouldActivate(rit)) {
            val newInstance = createInstance(rit)
            AtomicReference(
                ActiveAddressable(
                    instance = newInstance,
                    lastActivity = clock.currentTime
                )
            )
        } else {
            null
        }
    }

    private fun shouldActivate(rit: RemoteInvocationTarget): Boolean {
        return false
    }

    private fun createInstance(rit: RemoteInvocationTarget): Any {
        val newInstanceType = capabilitiesScanner.interfaceLookup.getValue(rit.interfaceDefinition.interfaceClass)
        return newInstanceType.getDeclaredConstructor().newInstance()
    }
}