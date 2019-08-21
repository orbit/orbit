/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.*
import orbit.server.routing.*

class ManualAddressablePlacementStrategy(var nextNode: NodeId = NodeId("")) : AddressablePlacementStrategy {
    override fun chooseNode(address: BaseAddress): NodeId {
        return nextNode
    }
}
