/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.*
import orbit.server.routing.MeshNode
import orbit.server.routing.Route

class LocalRemoteNode(
    override val id: NodeId = NodeId.generate(),
    val nodeDirectory: InMemoryNodeDirectory,
    val forwardMessage: (BaseMessage) -> Unit
) : MeshNode {
    override fun <T : BaseAddress> canHandle(address: T): Boolean {
        return true
    }

    override val capabilities = listOf(Capability.Routing)

    override fun sendMessage(message: BaseMessage, route: Route) {
        println("Send Message through node \"${this.id}\"")

        forwardMessage(message)
    }

    fun connect(client: MeshNode){
        nodeDirectory.connectNode(client, this.id)
    }
}
