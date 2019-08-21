/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.*
import orbit.server.net.NodeId
import orbit.server.routing.AddressableDirectory

class InMemoryAddressableDirectory : AddressableDirectory {
    private val directory = HashMap<BaseAddress, NodeId>()

    override fun lookup(address: BaseAddress): NodeId? {
        return directory[address]
    }

    override fun setLocation(address: BaseAddress, node: NodeId) {
        directory[address] = node
    }
}
