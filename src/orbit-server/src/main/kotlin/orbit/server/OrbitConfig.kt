/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import kotlinx.coroutines.CoroutineDispatcher
import orbit.common.concurrent.Pools
import orbit.server.local.InMemoryNodeDirectory
import orbit.server.net.NodeId
import orbit.server.routing.NodeDirectory

data class OrbitConfig(
    /**
     * The node's identity.
     */
    val nodeId: NodeId = NodeId.generate(),

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
    val pipelineBufferCount: Int = 10_000
)
