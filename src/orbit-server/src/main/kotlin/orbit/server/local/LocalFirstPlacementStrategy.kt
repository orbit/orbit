/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.addressable.AddressableReference
import orbit.server.net.NodeId
import orbit.server.routing.AddressableDirectory
import orbit.server.routing.AddressablePlacementStrategy
import orbit.server.routing.LocalNodeInfo
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo

internal class LocalFirstPlacementStrategy(val nodeDirectory: NodeDirectory, val addressableDirectory: AddressableDirectory, val localNode: LocalNodeInfo) : AddressablePlacementStrategy {
    override suspend fun chooseNode(address: AddressableReference): NodeId {
        val nodeId = nodeDirectory.lookupConnectedNodes(localNode.nodeInfo.id).filterIsInstance<NodeInfo.ClientNodeInfo>().first().id
        println("Choose placement: ${address.id} on ${nodeId}")
        addressableDirectory.setLocation(address, nodeId)
        return nodeId
    }
}
