/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.remoting

import cloud.orbit.common.time.TimeMs
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.key.Key
import java.lang.reflect.Method

/**
 * Marker interface that determines an interface is addressable remotely.
 */
@NonConcrete
interface Addressable

/**
 * A class type which extends [Addressable].
 */
typealias AddressableClass = Class<out Addressable>

/**
 * An [Addressable] which is currently activated in memory.
 */
data class ActiveAddressable(
    /**
     * The instance of the [Addressable].
     */
    val instance: Addressable,
    /**
     * The last time there was activity in this [Addressable].
     */
    val lastActivity: TimeMs,
    /**
     * The reference used to activate this addressable
     */
    val addressableReference: AddressableReference
)

/**
 * A special class for [Addressable]s which are created by Orbit which makes additional context available at runtime.
 */
abstract class ActivatedAddressable {
    data class AddressableContext(val reference: AddressableReference)
    lateinit var context: AddressableContext
}

/**
 * A reference to a specific addressable.
 */
data class AddressableReference(
    /**
     * The [Addressable] interface being referenced.
     */
    val interfaceClass: AddressableClass,
    /**
     * A unique key.
     */
    val key: Key
)

/**
 * An invocation of a method on an [Addressable].
 */
data class AddressableInvocation(
    /**
     * A reference to the [Addressable].
     */
    val reference: AddressableReference,
    /**
     * The method being called.
     */
    val method: Method,
    /**
     * The arguments being passed.
     */
    val args: Array<out Any?>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AddressableInvocation

        if (reference != other.reference) return false
        if (method != other.method) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = reference.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}