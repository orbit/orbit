/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import io.rouz.grpc.ManyToManyCall
import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.client.OrbitClientConfig
import orbit.shared.net.Message
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.Messages
import orbit.shared.proto.openStream
import orbit.shared.proto.toMessage
import orbit.shared.proto.toMessageProto
import orbit.util.concurrent.RailWorker
import orbit.util.concurrent.SupervisorScope

class ConnectionHandler(config: OrbitClientConfig, grpcClient: GrpcClient, private val scope: SupervisorScope) {
    private val logger = KotlinLogging.logger { }
    private val messagesStub = ConnectionGrpc.newStub(grpcClient.channel)

    private val messageRails = RailWorker(
        scope = scope,
        buffer = config.bufferCount,
        railCount = config.railCount,
        logger = logger,
        onMessage = this::onMessage
    )

    private lateinit var connectionChannel: ManyToManyCall<Messages.MessageProto, Messages.MessageProto>

    fun connect() {
        messageRails.startWorkers()
        connectionChannel = messagesStub.openStream()

        scope.launch {
            for (msg in connectionChannel) {
                messageRails.send(msg.toMessage())
            }
        }
    }

    private suspend fun onMessage(message: Message) {
        println(message)
    }

    fun send(msg: Message) {
        connectionChannel.send(msg.toMessageProto())
    }

    fun disconnect() {
        connectionChannel.cancel()
        messageRails.stopWorkers()
    }
}