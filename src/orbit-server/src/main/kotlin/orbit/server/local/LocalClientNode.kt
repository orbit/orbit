/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.*
import orbit.server.net.NodeId
import orbit.server.routing.*

class LocalClientNode<TAddress : BaseAddress>(
    override val id: NodeId = NodeId.generate(),
    override val capabilities: List<Capability> = listOf(),
    private val onClientMessage: (BaseMessage) -> Unit = {}
) : MeshNode {
    override fun sendMessage(message: BaseMessage, route: Route) {
        println("> ${this.id}: \"${message.content}\"")
    }

    fun onMessage(message: BaseMessage) {
        onClientMessage(message);
    }

    override fun <T : BaseAddress> canHandle(address: T): Boolean {
        return this.capabilities.contains(address.capability())
    }
}
