/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.concurrent.atomicSet
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
        val reference = getOrCreateAddressable(remoteInvocation.target)
        val active = reference?.get()!!

        // Call
        val rawResult = remoteInvocation.method.invoke(active.instance, *remoteInvocation.args)
        try {
            val result = DeferredWrappers.wrapCall(rawResult).await()
            completion.complete(result)
        } catch (t: Throwable) {
            completion.completeExceptionally(t)
        }

        // Update last active
        reference.atomicSet {
            it.copy(lastActivity = clock.currentTime)
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
            throw IllegalArgumentException(
                "Active addressable does not exist and can not be created. $rit"
            )
        }
    }

    private fun shouldActivate(rit: RemoteInvocationTarget): Boolean {
        return true
    }

    private fun createInstance(rit: RemoteInvocationTarget): Any {
        val newInstanceType = capabilitiesScanner.interfaceLookup.getValue(rit.interfaceClass)
        return newInstanceType.getDeclaredConstructor().newInstance()
    }
}