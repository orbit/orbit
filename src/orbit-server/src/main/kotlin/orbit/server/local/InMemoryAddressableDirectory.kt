/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.addressable.AddressableReference
import orbit.server.net.NodeId
import orbit.server.routing.AddressableDirectory

class InMemoryAddressableDirectory : AddressableDirectory {
    companion object Singleton {
        @JvmStatic
        private var directory: MutableMap<AddressableReference, NodeId> = HashMap()
    }

    suspend override fun lookup(address: AddressableReference): NodeId? {
        return directory[address]
    }

    suspend override fun setLocation(address: AddressableReference, node: NodeId) {
        directory[address] = node
    }

    override fun removeNode(node: NodeId) {
        val directoryCount = directory.count()
        directory = directory.filter { (address, nodeId) -> nodeId != node }.toMutableMap()

        if (directory.count() < directoryCount) {
            // TODO (brett) - Remove this diagnostic message
            println("Reducing addressable dictinary from $directoryCount to ${directory.count()}")
        }
    }
}
