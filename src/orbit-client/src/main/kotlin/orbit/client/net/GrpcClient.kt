/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import orbit.client.OrbitClientConfig

class GrpcClient(config: OrbitClientConfig) {
    private val endpoint = config.endpoint

    val channel: Channel = ManagedChannelBuilder
        .forAddress(endpoint.host, endpoint.port)
        .usePlaintext()
        .build()

}