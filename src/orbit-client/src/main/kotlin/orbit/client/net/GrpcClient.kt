/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import io.grpc.Channel
import io.grpc.ManagedChannelBuilder

internal class GrpcClient(nodeStatus: NodeStatus, authInterceptor: AuthInterceptor) {
    val channel: Channel = ManagedChannelBuilder
        .forAddress(nodeStatus.serviceLocator.host, nodeStatus.serviceLocator.port)
        .usePlaintext()
        .intercept(authInterceptor)
        .build()
}