/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import orbit.client.OrbitClientConfig

internal class GrpcClient(localNode: LocalNode, authInterceptor: ClientAuthInterceptor, config: OrbitClientConfig) {
    val channel: Channel = ManagedChannelBuilder
        .forTarget(localNode.status.grpcEndpoint)
        .usePlaintext()
        .intercept(authInterceptor)
        .enableRetry()
        .maxRetryAttempts(config.networkRetryAttempts)
        .build()
}