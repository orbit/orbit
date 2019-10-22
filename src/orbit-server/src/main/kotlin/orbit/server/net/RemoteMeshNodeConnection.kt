/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.shared.net.HostInfo

class RemoteMeshNodeConnection(hostInfo: HostInfo) {
    init {

    }

}

//internal class GrpcMeshNodeClient(override val id: NodeId, private val channel: ManagedChannel) : MeshNode {
//    private val sender =
//        ConnectionGrpc.newStub(ClientInterceptors.intercept(channel, NodeIdClientInterceptor(id))).messages()
//
//    init {
//        fun notify(channel: ManagedChannel) {
//            println("Channel state: ${id.value}: ${channel.getState(false)}")
//            channel.notifyWhenStateChanged(channel.getState(true)) { notify(channel) }
//        }
//
//        notify(channel)
//    }
//
//    constructor(id: NodeId, host: String, port: Int) : this(
//        id,
//        ManagedChannelBuilder.forAddress(host, port)
//            .usePlaintext()
//            .build()
//    )
//
//    override suspend fun sendMessage(message: Message, route: Route?) {
//        sender.send(message.toProto())
//    }
//}