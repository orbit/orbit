/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import orbit.shared.mesh.NodeId
import orbit.shared.proto.Headers

internal class TestAuthInterceptor(private val getNodeId: () -> NodeId) :
    ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object :
            ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>?, headers: Metadata) {
                val nodeId = getNodeId()
                headers.put(NAMESPACE, nodeId.namespace)
                headers.put(NODE_KEY, nodeId.key)

                super.start(responseListener, headers)
            }
        }
    }

    companion object {
        private val NAMESPACE = Metadata.Key.of(
            Headers.NAMESPACE_NAME,
            Metadata.ASCII_STRING_MARSHALLER
        )
        private val NODE_KEY = Metadata.Key.of(
            Headers.NODE_KEY_NAME,
            Metadata.ASCII_STRING_MARSHALLER
        )
    }
}