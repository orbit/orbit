/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.actor

import orbit.client.addressable.AddressableProxyFactory
import orbit.shared.addressable.Key

class ActorProxyFactory internal constructor(
    private val addressableProxyFactory: AddressableProxyFactory
) {
    private fun <T : Actor> createProxyInternal(type: Class<T>, key: Key): T =
        addressableProxyFactory.createProxy(type, key)

    /**
     * Gets a reference to an [Actor].
     *
     * @param T The type of [Actor].
     * @return The [Actor] reference.
     */
    fun <T : ActorWithNoKey> createProxy(grainType: Class<T>): T =
        createProxyInternal(grainType, Key.none())

    /**
     * Gets a reference to an [Actor] with a string key.
     *
     * @param T The type of [Actor].
     * @param key The key.
     * @return The [Actor] reference.
     */
    fun <T : ActorWithStringKey> createProxy(grainType: Class<T>, key: String): T =
        createProxyInternal(grainType, Key.of(key))

    /**
     * Gets a reference to an [Actor] with an int32 key.
     *
     * @param T The type of [Actor].
     * @param key The key.
     * @return The [Actor] reference.
     */
    fun <T : ActorWithInt32Key> createProxy(grainType: Class<T>, key: Int): T =
        createProxyInternal(grainType, Key.of(key))

    /**
     * Gets a reference to an [Actor] with an int64 key.
     *
     * @param T The type of [Actor].
     * @param key The key.
     * @return The [Actor] reference.
     */
    fun <T : ActorWithInt64Key> createProxy(grainType: Class<T>, key: Long): T =
        createProxyInternal(grainType, Key.of(key))
}

/**
 * Gets a reference to an [Actor] with no key.
 *
 * @param T The type of [Actor].
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithNoKey> ActorProxyFactory.createProxy(): T =
    createProxy(T::class.java)

/**
 * Gets a reference to an [Actor] with a string key.
 *
 * @param T The type of [Actor].
 * @param key The key.
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithStringKey> ActorProxyFactory.createProxy(key: String): T =
    createProxy(T::class.java, key)

/**
 * Gets a reference to an [Actor] with an int32 key.
 *
 * @param T The type of [Actor].
 * @param key The key.
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithInt32Key> ActorProxyFactory.createProxy(key: Int): T =
    createProxy(T::class.java, key)

/**
 * Gets a reference to an [Actor] with a int64 key.
 *
 * @param T The type of [Actor].
 * @param key The key.
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithInt64Key> ActorProxyFactory.createProxy(key: Long): T =
    createProxy(T::class.java, key)
