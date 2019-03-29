/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.net

sealed class NetTarget {
    data class Unicast(val targetNode: NodeIdentity) : NetTarget()
    data class Multicast(val nodes: Iterable<NodeIdentity>) : NetTarget() {
        constructor(vararg nodes: NodeIdentity) : this(nodes.asIterable())
    }

    object Broadcast : NetTarget()
}