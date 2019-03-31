/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.remoting

import cloud.orbit.core.key.Key
import cloud.orbit.core.net.NetTarget
import java.util.concurrent.CompletableFuture

interface AddressableRegistry {
    fun registerAddressable(reference: AddressableReference, instance: Addressable): CompletableFuture<Unit>

    @JvmDefault
    fun registerAddressable(interfaceClass: AddressableClass, key: Key, instance: Addressable):
            CompletableFuture<Unit> = registerAddressable(AddressableReference(interfaceClass, key), instance)

    fun deregisterAddressable(instance: Addressable): CompletableFuture<Unit>

    fun <T : Addressable> getReference(interfaceClass: Class<T>, key: Key, target: NetTarget? = null): T
}

inline fun <reified T : Addressable> AddressableRegistry.registerAddressable(key: Key, instance: Addressable) =
    registerAddressable(T::class.java, key, instance)

inline fun <reified T : Addressable> AddressableRegistry.getReference(key: Key, target: NetTarget? = null) =
    getReference(T::class.java, key, target)