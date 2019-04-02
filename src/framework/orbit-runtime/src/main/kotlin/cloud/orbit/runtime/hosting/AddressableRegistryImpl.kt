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
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.remoting.AddressableInterfaceClientProxyFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture

internal class AddressableRegistryImpl(
    private val proxyFactory: AddressableInterfaceClientProxyFactory,
    private val executionSystem: ExecutionSystem,
    private val supervisorScope: SupervisorScope,
    private val netSystem: NetSystem
) : AddressableRegistry {

    override fun <T : Addressable> registerAddressable(
        interfaceClass: Class<T>,
        key: Key,
        instance: T
    ): CompletableFuture<T> = supervisorScope.async {
        registerAddressableInternal(AddressableReference(interfaceClass = interfaceClass, key = key), instance)
        createProxy(interfaceClass, key, netSystem.localNode.nodeIdentity.asTarget())
    }.asCompletableFuture()

    override fun deregisterAddressable(instance: Addressable) = supervisorScope.async {
        deregisterAddressableInternal(instance)
    }.asCompletableFuture()

    override fun <T : Addressable> createProxy(
        interfaceClass: Class<T>,
        key: Key,
        target: NetTarget?
    ): T =
        proxyFactory.createProxy(interfaceClass, key, target)


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