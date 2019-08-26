/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.Address
import orbit.server.Capability
import orbit.server.net.Message
import orbit.server.net.NodeId

internal class Mesh(override val id: NodeId, override val capabilities: List<Capability>) : MeshNode {
    override fun <T : Address> canHandle(address: T): Boolean {
        return true
    }

    override fun sendMessage(message: Message, route: Route) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object _Mesh {
        @JvmStatic
        val Instance: Mesh = Mesh(NodeId("mesh"), capabilities = listOf(Capability.Mesh))
    }
}