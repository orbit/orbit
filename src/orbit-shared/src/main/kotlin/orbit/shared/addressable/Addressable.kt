/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.addressable

import orbit.shared.mesh.Namespace
import orbit.shared.net.InvocationReason

typealias AddressableType = String

data class AddressableReference(
    val type: AddressableType,
    val key: Key
)

data class NamespacedAddressableReference(
    val namespace: Namespace,
    val addressableReference: AddressableReference
)

/**
 * An invocation of a method on an Addressable.
 */
data class AddressableInvocation(
    /**
     * A reference to the Addressable.
     */
    val reference: AddressableReference,

    /**
     * The method being called.
     */
    val method: String,
    /**
     * The arguments being passed.
     */
    val args: AddressableInvocationArguments,
    /**
     * Reason the invocation call was sent
     */
    val reason: InvocationReason = InvocationReason.invocation
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

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

typealias AddressableInvocationArguments = Array<out AddressableInvocationArgument>
typealias AddressableInvocationArgument = Pair<Any?, Class<*>>