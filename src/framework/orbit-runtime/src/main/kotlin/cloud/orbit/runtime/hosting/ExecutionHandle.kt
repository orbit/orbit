/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.time.Clock
import cloud.orbit.core.remoting.ActivatedAddressable
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import java.util.concurrent.atomic.AtomicLong

class ExecutionHandle(
    val instance: Addressable,
    val reference: AddressableReference,
    val definition: AddressableInterfaceDefinition,
    componentProvider: ComponentProvider
) {
    private val clock: Clock by componentProvider.inject()

    val createdTime = clock.currentTime

    private val lastActivityAtomic = AtomicLong(createdTime)
    val lastActivity get() = lastActivityAtomic.get()


    fun activate() {
        if (instance is ActivatedAddressable) {
            instance.context = ActivatedAddressable.AddressableContext(
                reference = reference
            )
        }
    }

    suspend fun invoke(
        invocation: AddressableInvocation,
        completion: Completion
    ) {
        lastActivityAtomic.set(clock.currentTime)
        val rawResult = invocation.method.invoke(instance, *invocation.args)
        try {
            val result = DeferredWrappers.wrapCall(rawResult).await()
            completion.complete(result)
        } catch (t: Throwable) {
            completion.completeExceptionally(t)
        }
    }

    fun deactivate() {

    }
}