/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.shared.proto.Headers

class ServerAuthInterceptor : ServerInterceptor {
    companion object Keys {
        @JvmStatic
        val NODE_ID = Context.key<NodeId>(Headers.NODE_ID_NAME)

        @JvmStatic
        val NAMESPACE = Context.key<Namespace>(Headers.NAMESPACE_NAME)
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {

        val nodeId = headers.get(Metadata.Key.of(Headers.NODE_ID_NAME, Metadata.ASCII_STRING_MARSHALLER))
        val namespace = headers.get(Metadata.Key.of(Headers.NAMESPACE_NAME, Metadata.ASCII_STRING_MARSHALLER))

        val context = Context.current().let {
            if (namespace != null) it.withValue(NAMESPACE, namespace) else it
        }.let {
            if (nodeId != null) it.withValue(NODE_ID, NodeId(nodeId)) else it
        }

        return Contexts.interceptCall(context, call, headers, next)
    }
}