/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import io.rouz.grpc.ManyToManyCall
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages
import orbit.shared.proto.messages

internal class ConnectionHandler(grpcClient: GrpcClient) {
    private val messagesStub = ConnectionGrpc.newStub(grpcClient.channel)

    private lateinit var connectionChannel: ManyToManyCall<Messages.Message, Messages.Message>

    fun connect() {
        connectionChannel = messagesStub.messages()
    }

    fun disconnect() {
        connectionChannel.cancel()
    }

}