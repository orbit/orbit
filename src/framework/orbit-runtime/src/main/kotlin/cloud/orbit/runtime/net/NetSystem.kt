/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.core.net.NodeInfo

internal class NetSystem {
    val localNodeManipulator = NodeManipulator()
    val localNode: NodeInfo get() = localNodeManipulator.nodeInfo
    val clusterNodes: List<NodeInfo> get() = listOf(localNode)
}