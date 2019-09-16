/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.net.NodeId

internal interface NodeDirectory {
    suspend fun <TNodeInfo : NodeInfo> join(nodeInfo: TNodeInfo): TNodeInfo
    suspend fun report(nodeInfo: NodeInfo)
    fun removeNode(nodeId: NodeId)
    fun getNode(nodeId: NodeId): NodeInfo?

    fun lookupConnectedNodes(nodeId: NodeId): Sequence<NodeInfo>
    fun lookupMeshNodes(): List<NodeInfo.ServerNodeInfo>
}
