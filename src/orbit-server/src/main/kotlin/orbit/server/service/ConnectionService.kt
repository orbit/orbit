/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import orbit.server.concurrent.RuntimeScopes
import orbit.server.net.ConnectionManager
import orbit.shared.proto.ConnectionImplBase
import orbit.shared.proto.Messages


class ConnectionService(
    private val connectionManager: ConnectionManager,
    runtimeScopes: RuntimeScopes
) : ConnectionImplBase(runtimeScopes.ioScope.coroutineContext) {
    override fun openStream(requests: ReceiveChannel<Messages.MessageProto>): ReceiveChannel<Messages.MessageProto> {
        val outboundChannel = Channel<Messages.MessageProto>()
        val nodeId = ServerAuthInterceptor.NODE_ID.get()
        val namespace = ServerAuthInterceptor.NAMESPACE.get()

        connectionManager.onNewClient(namespace, nodeId, requests, outboundChannel)
        return outboundChannel
    }
}