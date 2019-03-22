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

class ExecutionSystem(private val clock: Clock,
                      private val capabilitiesScanner: CapabilitiesScanner) {
    data class ActiveAddressable(
        val lastActivity: TimeMs,
        val instance: Any? = null
    )

    private val activeAddressables = ConcurrentHashMap<RemoteInvocationTarget, AtomicReference<ActiveAddressable>>()

    suspend fun handleInvocation(remoteInvocation: RemoteInvocation, completion: Completion) {
        val addressableRef = getOrCreateRef(remoteInvocation.target)
        var active = addressableRef.get()!!

        // Activation
        if(active.instance == null) {
            active = addressableRef.atomicSet {
                it.copy(instance = createInstance(remoteInvocation.target))
            }
        }

        // Call
        val rawResult = remoteInvocation.methodDefinition.method.invoke(active.instance, *remoteInvocation.args)
        try {
            val result = DeferredWrappers.wrapCall(rawResult).await()
            completion.complete(result)
        } catch(t: Throwable) {
            completion.completeExceptionally(t)
        }
    }

    private fun createInstance(rit: RemoteInvocationTarget): Any {
        val newInstanceType = capabilitiesScanner.interfaceLookup.getValue(rit.interfaceDefinition.interfaceClass)
        return newInstanceType.getDeclaredConstructor().newInstance()
    }

    private fun getOrCreateRef(rit: RemoteInvocationTarget) = activeAddressables.getOrPut(rit) {
        AtomicReference(
            ActiveAddressable(
                lastActivity = clock.currentTime
            )
        )
    }
}