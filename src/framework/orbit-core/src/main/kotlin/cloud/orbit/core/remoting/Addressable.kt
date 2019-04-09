/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.remoting

import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.key.Key
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.runtime.RuntimeContext
import java.lang.reflect.Method


@NonConcrete
/**
 * Marker interface that determines an interface is addressable remotely.
 */
interface Addressable

/**
 * A class type which extends [Addressable].
 */
typealias AddressableClass = Class<out Addressable>

/**
 * An abstract [Addressable] which allows Orbit to provide an [AddressableContext].
 */
abstract class AbstractAddressable {
    /**
     * The Orbit context. It will be available after the [Addressable] is registered with Orbit.
     * Attempting to access this variable before registration is undefined behavior.
     */
    lateinit var context: AddressableContext
}

/**
 * A context available to an [Addressable] which gives access to Orbit runtime information.
 */
data class AddressableContext(
    /**
     * A reference to this [Addressable].
     */
    val reference: AddressableReference,
    /**
     * A reference to the Orbit [RuntimeContext].
     */
    val runtime: RuntimeContext
)


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
 * A reference to a specific addressable on a specific target.
 */
data class RemoteAddressableReference(
    /**
     * The referenced addressable.
     */
    val reference: AddressableReference,
    /**
     * The current known target.
     */
    val target: NetTarget
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
     * The type of invocation.
     */
    val invocationType: AddressableInvocationType,
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
        if (invocationType != other.invocationType) return false
        if (method != other.method) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = reference.hashCode()
        result = 31 * result + invocationType.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}

enum class AddressableInvocationType {
    REQUEST_RESPONSE,
    ONE_WAY
}