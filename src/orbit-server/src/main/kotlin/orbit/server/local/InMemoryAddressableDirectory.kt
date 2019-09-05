/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.Address
import orbit.server.net.NodeId
import orbit.server.routing.AddressableDirectory

class InMemoryAddressableDirectory : AddressableDirectory {
    companion object Singleton {
        @JvmStatic
        private val directory = HashMap<Address, NodeId>()
    }


    override fun lookup(address: Address): NodeId? {
        return directory[address]
    }

    override fun setLocation(address: Address, node: NodeId) {
        directory[address] = node
    }
}
