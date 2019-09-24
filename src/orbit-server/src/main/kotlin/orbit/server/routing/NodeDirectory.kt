/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.net.NodeId

interface NodeDirectory {
    interface NodeDirectoryConfig {
        val directoryType: Class<out NodeDirectory>
        val specificConfig: Any?
    }

    suspend fun <TNodeInfo : NodeInfo> join(nodeInfo: TNodeInfo): TNodeInfo
    suspend fun report(nodeInfo: NodeInfo)
    suspend fun getNode(nodeId: NodeId): NodeInfo?

    suspend fun lookupConnectedNodes(nodeId: NodeId): List<NodeInfo>
    suspend fun lookupMeshNodes(): List<NodeInfo.ServerNodeInfo>
    suspend fun cullLeases()
}
