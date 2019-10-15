/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.mesh

data class NodeInfo(
    val id: NodeId,
    val capabilities: NodeCapabilities,
    val visibleNodes: Set<NodeId> = emptySet(),
    val lease: NodeLease
)