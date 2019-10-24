/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.mesh

import orbit.shared.net.HostInfo

data class NodeInfo(
    val id: NodeId,
    val capabilities: NodeCapabilities,
    val hostInfo: HostInfo? = null,
    val lease: NodeLease,
    val visibleNodes: Set<NodeId> = emptySet()
)
