/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.routing

import orbit.server.net.Message
import orbit.server.net.NodeId

internal class Mesh(private val nodeDirectory: NodeDirectory) : MeshNode {
    companion object Id {
        val NodeId = NodeId("mesh")
    }

    override val id = NodeId
    override fun sendMessage(message: Message, route: Route?) {
        val (nextRoute, nextNode) = route!!.pop()
//        val nodeInfo = nodeDirectory.getNode(nextNode)?.sendMessage(message, nextRoute)
    }
}