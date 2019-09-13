/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import kotlinx.coroutines.CoroutineDispatcher
import orbit.common.concurrent.Pools
import java.time.Duration

data class OrbitServerConfig(
    /**
     * The gRPC endpoint port.
     */
    val grpcPort: Int = 50056,

    /**
     * The Orbit tick rate in milliseconds.
     */
    val tickRate: Long = 1000,

    /**
     * The pool where CPU intensive tasks will run.
     */
    val cpuPool: CoroutineDispatcher = Pools.createFixedPool("orbit-cpu"),

    /**
     * The pool where IO intensive tasks will run.
     */
    val ioPool: CoroutineDispatcher = Pools.createCachedPool("orbit-io"),

    /**
     * The number of workers that can process a message concurrently.
     */
    val pipelineRailCount: Int = 128,

    /**
     * The number of messages (either inbound or outbound) that may be queued before new messages are rejected.
     */
    val pipelineBufferCount: Int = 10_000,

    /**
     * Prevents the JVM shutting down when the main thread exits.
     */
    val acquireShutdownLatch: Boolean = true,

    /**
     * The duration of a client lease
     */
    val leaseExpiration: Duration = Duration.ofSeconds(60),

    /**
     * The duration before a client lease renewal
     */
    val leaseRenewal: Duration = Duration.ofSeconds(30)
)
