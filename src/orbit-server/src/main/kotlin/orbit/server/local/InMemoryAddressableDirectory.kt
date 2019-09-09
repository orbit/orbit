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
        private val directory = HashMap<AddressableReference, NodeId>()
    }


    suspend override fun lookup(address: AddressableReference): NodeId? {
        return directory[address]
    }

    suspend override fun setLocation(address: AddressableReference, node: NodeId) {
        directory[address] = node
    }
}
