/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.grpc.stub.StreamObserver
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.ConnectionOuterClass

class ConnectionImpl(val server: OrbitServer) : ConnectionGrpc.ConnectionImplBase() {
    override fun sendMessage(
        request: ConnectionOuterClass.MessageRequest,
        responseObserver: StreamObserver<ConnectionOuterClass.MessageReply>
    ) {
        this.server.handleMessage(Message(request.content, Address(AddressId(request.destination))))
        responseObserver.onNext(ConnectionOuterClass.MessageReply.newBuilder().setMessage("Message sent").build())
        responseObserver.onCompleted()
    }
}