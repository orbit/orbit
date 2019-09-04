/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

import orbit.server.*
import orbit.server.net.Message
import orbit.server.net.NodeId
import orbit.server.routing.*

internal class LocalClientNode(
    override val id: NodeId = NodeId.generate("client"),
    private val onClientMessage: (Message) -> Unit = {}
) : MeshNode {
    override fun sendMessage(message: Message, route: Route?) {
        println("> ${this.id}: \"${message.content}\"")
    }

    fun onMessage(message: Message) {
        onClientMessage(message);
    }
}
