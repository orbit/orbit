/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.server.*
import orbit.server.routing.*
import orbit.shared.proto.*
import java.time.LocalDateTime
import kotlin.concurrent.timer

class GrpcRemoteNode(override val id: NodeId, override val capabilities: List<Capability>, val server: OrbitServer) :
    ConnectionGrpc.ConnectionImplBase(), MeshNode {
    override fun sendMessage(message: BaseMessage, route: Route) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openMessageStream(responseObserver: StreamObserver<ConnectionOuterClass.MessageStreamResponse>): StreamObserver<ConnectionOuterClass.MessageStreamRequest> {
        responseObserver.onNext(ConnectionOuterClass.MessageStreamResponse.newBuilder().setMessage("responding").build())

        val tim = timer("resp", true, period = 5000) {
            println("Updating client")
            responseObserver.onNext(ConnectionOuterClass.MessageStreamResponse.newBuilder().setMessage("Msg: ${LocalDateTime.now()}").build())
        }

        return object : StreamObserver<ConnectionOuterClass.MessageStreamRequest> {
            override fun onError(t: Throwable?) {
                println("stream error")
                tim.cancel()
                responseObserver.onError(t)
            }

            override fun onCompleted() {
                println("stream complete")
                tim.cancel()
                responseObserver.onCompleted()
            }

            override fun onNext(value: ConnectionOuterClass.MessageStreamRequest) {
                handleMessage(value.message, Address(AddressId(value.address)))
            }

        }
    }

    fun handleMessage(content: String, address: Address) {
        server.handleMessage(Message(content, address))
    }

    override fun <T : Address> canHandle(address: T): Boolean {
        return true
    }

}