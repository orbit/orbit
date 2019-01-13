/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.actor

import cloud.orbit.core.key.Key
import java.util.*

/**
 * A proxy factory for generating references to [Actor]'s.
 */
interface ActorProxyFactory {
    fun <T : Actor> getReferenceInternal(grainType: Class<T>, grainKey: Key): T

    /**
     * Gets a reference to an [Actor].
     *
     * @param T The type of [Actor].
     * @return The [Actor] reference.
     */
    @JvmDefault
    fun <T : ActorWithNoKey> getReference(grainType: Class<T>): T =
        getReferenceInternal(grainType, Key.none())

    /**
     * Gets a reference to an [Actor] with a string key.
     *
     * @param T The type of [Actor].
     * @param key The key.
     * @return The [Actor] reference.
     */
    @JvmDefault
    fun <T : ActorWithStringKey> getReference(grainType: Class<T>, key: String): T =
        getReferenceInternal(grainType, Key.of(key))

    /**
     * Gets a reference to an [Actor] with an int32 key.
     *
     * @param T The type of [Actor].
     * @param key The key.
     * @return The [Actor] reference.
     */
    @JvmDefault
    fun <T : ActorWithInt32Key> getReference(grainType: Class<T>, key: Int): T =
        getReferenceInternal(grainType, Key.of(key))

    /**
     * Gets a reference to an [Actor] with an int64 key.
     *
     * @param T The type of [Actor].
     * @param key The key.
     * @return The [Actor] reference.
     */
    @JvmDefault
    fun <T : ActorWithInt64Key> getReference(grainType: Class<T>, key: Long): T =
        getReferenceInternal(grainType, Key.of(key))

    /**
     * Gets a reference to an [Actor] with a guid key.
     *
     * @param T The type of [Actor].
     * @param key The key.
     * @return The [Actor] reference.
     */
    @JvmDefault
    fun <T : ActorWithGuidKey> getReference(grainType: Class<T>, key: UUID): T =
        getReferenceInternal(grainType, Key.of(key))
}

/**
 * Gets a reference to an [Actor] with no key.
 *
 * @param T The type of [Actor].
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithNoKey> ActorProxyFactory.getReference(): T =
    getReference(T::class.java)

/**
 * Gets a reference to an [Actor] with a string key.
 *
 * @param T The type of [Actor].
 * @param key The key.
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithStringKey> ActorProxyFactory.getReference(key: String): T =
    getReference(T::class.java, key)

/**
 * Gets a reference to an [Actor] with an int32 key.
 *
 * @param T The type of [Actor].
 * @param key The key.
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithInt32Key> ActorProxyFactory.getReference(key: Int): T =
    getReference(T::class.java, key)

/**
 * Gets a reference to an [Actor] with a int64 key.
 *
 * @param T The type of [Actor].
 * @param key The key.
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithInt64Key> ActorProxyFactory.getReference(key: Long): T =
    getReference(T::class.java, key)

/**
 * Gets a reference to an [Actor] with a guid key.
 *
 * @param T The type of [Actor].
 * @param key The key.
 * @return The [Actor] reference.
 */
inline fun <reified T : ActorWithGuidKey> ActorProxyFactory.getReference(key: UUID): T =
    getReference(T::class.java, key)
