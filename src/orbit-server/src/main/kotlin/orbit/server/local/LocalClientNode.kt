/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.*
import orbit.server.net.Message
import orbit.server.net.NodeId
import orbit.server.routing.*

internal class LocalClientNode<TAddress : Address>(
    override val id: NodeId = NodeId.generate(),
    override val capabilities: List<Capability> = listOf(),
    private val onClientMessage: (Message) -> Unit = {}
) : MeshNode {
    override fun sendMessage(message: Message, route: Route) {
        println("> ${this.id}: \"${message.content}\"")
    }

    fun onMessage(message: Message) {
        onClientMessage(message);
    }

    override fun <T : Address> canHandle(address: T): Boolean {
        return this.capabilities.contains(address.capability())
    }
}
