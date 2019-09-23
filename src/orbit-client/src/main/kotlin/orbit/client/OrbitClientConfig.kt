/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.CoroutineDispatcher
import orbit.common.concurrent.Pools
import java.net.URI
import java.time.Duration

data class OrbitClientConfig(
    /**
     * The gRPC endpoint of the Orbit server.
     */
    val endpoint: URI = URI("grpc://localhost:50056"),

    /**
     * The tick rate of the Orbit client.
     */
    val tickRate: Duration = Duration.ofSeconds(1),

    /**
     * The pool where Orbit client tasks will run.
     */
    val pool: CoroutineDispatcher = Pools.createFixedPool("orbit-client")
)