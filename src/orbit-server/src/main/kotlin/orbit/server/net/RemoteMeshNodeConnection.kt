/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import orbit.server.mesh.LocalNodeInfo
import orbit.server.service.ClientAuthInterceptor
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.net.Message
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.openStream
import orbit.shared.proto.toMessageProto
import orbit.shared.router.Route

class RemoteMeshNodeConnection(localNode: LocalNodeInfo, val id: NodeId, channel: ManagedChannel) : MessageSender {
    private val sender =
        ConnectionGrpc.newStub(ClientInterceptors.intercept(channel, ClientAuthInterceptor(localNode))).openStream()

    init {
        fun notify(channel: ManagedChannel) {
            channel.notifyWhenStateChanged(channel.getState(true)) { notify(channel) }
        }

        notify(channel)
    }

    constructor(localNode: LocalNodeInfo, remoteNode: NodeInfo) : this(
        localNode,
        remoteNode.id,
        ManagedChannelBuilder.forTarget(remoteNode.url)
            .usePlaintext()
            .build()
    )

    override suspend fun sendMessage(message: Message, route: Route?) {
        sender.send(message.toMessageProto())
    }
}
