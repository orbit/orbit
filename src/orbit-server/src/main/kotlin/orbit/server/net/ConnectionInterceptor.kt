/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler

class ConnectionInterceptor : io.grpc.ServerInterceptor {

    companion object Keys {
        @JvmStatic
        val NODE_ID = Context.key<String>("nodeId")
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {

        val nodeId = headers.get(Metadata.Key.of("nodeId", Metadata.ASCII_STRING_MARSHALLER))

        val context = Context.current().withValue(NODE_ID, nodeId)
        return Contexts.interceptCall(context, call, headers, next)
    }
}