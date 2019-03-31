/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.key.Key
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.core.remoting.AddressableRegistry
import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.remoting.AddressableInterfaceClientProxyFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture

internal class AddressableRegistryImpl(
    private val proxyFactory: AddressableInterfaceClientProxyFactory,
    private val executionSystem: ExecutionSystem,
    private val supervisorScope: SupervisorScope
) : AddressableRegistry {
    override fun registerAddressable(reference: AddressableReference, instance: Addressable) = supervisorScope.async {
        registerAddressableInternal(reference, instance)
    }.asCompletableFuture()


    override fun deregisterAddressable(instance: Addressable) = supervisorScope.async {
        deregisterAddressableInternal(instance)
    }.asCompletableFuture()

    override fun <T : Addressable> getReference(
        interfaceClass: Class<T>,
        key: Key,
        target: NetTarget?
    ): T =
        proxyFactory.getReference(interfaceClass, key, target)


    private suspend fun registerAddressableInternal(reference: AddressableReference, instance: Addressable) {
        executionSystem.registerAddressableInstance(
            reference = reference,
            addressable = instance
        )
    }

    private suspend fun deregisterAddressableInternal(instance: Addressable) {
        executionSystem.deregisterAddressableInstance(instance)
    }
}