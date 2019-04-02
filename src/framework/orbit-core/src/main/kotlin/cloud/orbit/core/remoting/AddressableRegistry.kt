/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.remoting

import cloud.orbit.core.key.Key
import cloud.orbit.core.net.NetTarget
import java.util.concurrent.CompletableFuture

/**
 * The system for registering and managing [Addressable]s.
 */
interface AddressableRegistry {
    /**
     * Registers an [Addressable] instance with the current Orbit runtime.
     *
     * @param interfaceClass The interface of this addressable.
     * @param key The key that identitifies this addressable.
     * @param instance The instance of the addressable.
     * @return A future which is completed once registration is complete.
     */
    fun <T : Addressable> registerAddressable(interfaceClass: Class<T>, key: Key, instance: T): CompletableFuture<T>

    /**
     * Deregisters an active addressable from the current Orbit runtime.
     *
     * @param instance The instance of the addressable to deregister.
     * @return A future which is completed once deregistration is complete.
     */
    fun deregisterAddressable(instance: Addressable): CompletableFuture<Unit>

    /**
     * Create a proxy to the specified addressable.
     *
     * @param interfaceClass The interface of the addressable.
     * @param key The key that identifies the addressable.
     * @param target The optional (if known) target of calls to this addressable.
     * @return A proxy to the addressable.
     */
    fun <T : Addressable> createProxy(interfaceClass: Class<T>, key: Key, target: NetTarget? = null): T
}

/**
 * Registers an [Addressable] instance with the current Orbit runtime.
 *
 * @param T The interface of this addressable.
 * @param key The key that identitifies this addressable.
 * @param instance The instance of the addressable.
 * @return A future which is completed once registration is complete.
 */
inline fun <reified T : Addressable> AddressableRegistry.registerAddressable(key: Key, instance: T) =
    registerAddressable(T::class.java, key, instance)

/**
 * Create a proxy to the specified addressable.
 *
 * @param T The interface of the addressable.
 * @param key The key that identifies the addressable.
 * @param target The optional (if known) target of calls to this addressable.
 * @return A proxy to the addressable.
 */
inline fun <reified T : Addressable> AddressableRegistry.createProxy(key: Key, target: NetTarget? = null) =
    createProxy(T::class.java, key, target)