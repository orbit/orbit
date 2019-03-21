/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key

data class RemoteInvocationTarget(
    val interfaceDefinition: RemoteInterfaceDefinition,
    val key: Key
)

data class RemoteInvocation(
    val target: RemoteInvocationTarget,
    val methodDefinition: RemoteMethodDefinition,
    val args: Array<out Any?>
)  {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteInvocation

        if (target != other.target) return false
        if (methodDefinition != other.methodDefinition) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = target.hashCode()
        result = 31 * result + methodDefinition.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}