/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.mesh

import orbit.util.misc.RNGUtils

typealias Namespace = String
typealias NodeKey = String

data class NodeId(val key: NodeKey, val namespace: Namespace) {
    companion object {
        fun generate(namespace: Namespace): NodeId {
            return NodeId(RNGUtils.randomString(), namespace)
        }
    }

    override fun toString(): String {
        return "(${this.namespace}:${this.key})"
    }
}
