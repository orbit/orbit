/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.addressable.AddressableReference
import orbit.server.net.LocalNodeId
import orbit.server.net.NodeId
import orbit.server.routing.AddressableDirectory
import orbit.server.routing.AddressablePlacementStrategy
import orbit.server.routing.NodeDirectory

internal class LocalFirstPlacementStrategy(val nodeDirectory: NodeDirectory, val addressableDirectory: AddressableDirectory, val localNode: LocalNodeId) : AddressablePlacementStrategy {
    suspend override fun chooseNode(address: AddressableReference): NodeId {
        val nodeId = nodeDirectory.lookupConnectedNodes(localNode.nodeId).filterIsInstance<NodeDirectory.NodeInfo.ClientNodeInfo>().elementAt(0).id
        addressableDirectory.setLocation(address, nodeId)
        return nodeId
    }
}
