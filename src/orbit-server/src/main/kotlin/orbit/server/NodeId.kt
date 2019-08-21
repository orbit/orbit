/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import java.util.*

data class NodeId (private val value: String ) {
    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return other is NodeId && this.value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    companion object Generate {
        fun generate(): NodeId {
            return NodeId(UUID.randomUUID().toString())
        }
    }
}