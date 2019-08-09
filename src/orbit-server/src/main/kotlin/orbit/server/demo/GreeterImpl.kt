/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.demo

import orbit.shared.proto.GreeterGrpc
import orbit.shared.proto.GreeterOuterClass

class GreeterImpl : GreeterGrpc.GreeterImplBase() {
    override fun sayHello(
        request: GreeterOuterClass.HelloRequest,
        responseObserver: io.grpc.stub.StreamObserver<GreeterOuterClass.HelloReply>
    ) {
        val reply = GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello ${request.name}").build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}