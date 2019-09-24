/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor


internal class AuthInterceptor(private val nodeStatus: NodeStatus) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object :
            ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>?, headers: Metadata) {

                headers.put(NAMESPACE, nodeStatus.serviceLocator.namespace)

                nodeStatus.currentLease.get()?.nodeId?.let {
                    headers.put(NODE_ID, it)
                }

                super.start(responseListener, headers)
            }
        }
    }

    companion object {
        private val NAMESPACE = Metadata.Key.of("namespace", Metadata.ASCII_STRING_MARSHALLER)
        private val NODE_ID = Metadata.Key.of("nodeId", Metadata.ASCII_STRING_MARSHALLER)
    }
}