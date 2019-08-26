/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.stub.StreamObserver
import orbit.server.routing.Router
import orbit.shared.proto.ConnectionGrpc
import orbit.shared.proto.ConnectionOuterClass

internal class ClientConnections(val router: Router) : ConnectionGrpc.ConnectionImplBase() {

    private val clients = HashMap<NodeId, GrpcClient>()

    override fun messages(responseObserver: StreamObserver<ConnectionOuterClass.MessageStreamResponse>): StreamObserver<ConnectionOuterClass.Message> {
        val nodeId = NodeId(ConnectionInterceptor.NODE_ID.get())

        val connection = clients[nodeId] ?: GrpcClient(NodeId.generate(), listOf()) { msg ->
            router.routeMessage(msg)
        }
        clients[connection.id] = connection

        return connection
    }
}

//        return object : StreamObserver<ConnectionOuterClass.Message> {
//            override fun onError(t: Throwable?) {
//                println("stream error")
//                responseObserver.onError(t)
//            }
//
//            override fun onCompleted() {
//                println("stream complete")
//                responseObserver.onCompleted()
//            }
//
//            override fun onNext(value: ConnectionOuterClass.Message) {
//                println("Next: ${value.content}")
//            }
//        }//    override fun connect(
//        request: ConnectionOuterClass.MessageStreamRequest,
//        responseObserver: StreamObserver<ConnectionOuterClass.MessageStreamResponse>
//    ) {
//        var nodeId = NodeId.generate()
//        when (request.payloadCase) {
//            ConnectionOuterClass.MessageStreamRequest.PayloadCase.MESSAGE ->
//                println("Message sent on connect: ${request.message.content}")
//            ConnectionOuterClass.MessageStreamRequest.PayloadCase.NODEINFO -> {
//                nodeId = NodeId(request.nodeInfo.nodeId)
//                println("Received a connection message ${nodeId}")
//            }
//        }
//
//        val connection =
//            clients[nodeId] ?: GrpcClient(NodeId.generate(), listOf()) { msg -> println("grpc msg $msg") }
//        clients[connection.id] = connection
////        request.
//    }

//    override fun messages(responseObserver: StreamObserver<ConnectionOuterClass.MessageStreamResponse>?): StreamObserver<ConnectionOuterClass.MessageStreamRequest> {
//        val connection = clients

//        responseObserver

//        return super.messages(responseObserver)
//    }

//    override fun connect(responseObserver: StreamObserver<ConnectionOuterClass.MessageStreamResponse>): StreamObserver<ConnectionOuterClass.MessageStreamRequest> {
//        responseObserver.onNext(ConnectionOuterClass.MessageStreamResponse.newBuilder().setMessage("responding").build())

//        return GrpcClient(router.nodeId, responseObserver, )

//        return object : StreamObserver<ConnectionOuterClass.MessageStreamRequest> {
//            override fun onError(t: Throwable?) {
//                println("stream error")
//                responseObserver.onError(t)
//            }
//
//            override fun onCompleted() {
//                println("stream complete")
//                responseObserver.onCompleted()
//            }
//
//            override fun onNext(value: ConnectionOuterClass.MessageStreamRequest) {
//                handleMessage(value.message, Address(AddressId(value.address)))
//            }
//        }
//    }
