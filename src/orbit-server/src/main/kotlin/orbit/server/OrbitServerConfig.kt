/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import kotlinx.coroutines.CoroutineDispatcher
import orbit.common.concurrent.Pools
import orbit.server.etcd.EtcdNodeDirectory
import orbit.server.net.LeaseExpiration
import orbit.server.routing.NodeDirectory
import java.time.Duration

data class OrbitServerConfig(
    /**
     * The gRPC endpoint port.
     */
    val grpcPort: Int = System.getenv("ORBIT_PORT")?.toInt(10) ?: 50056,

    /**
     * The Orbit tick rate.
     */
    val tickRate: Duration = Duration.ofSeconds(1),

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
     * Expiration times for client leases
     */
    val leaseExpiration: LeaseExpiration = LeaseExpiration(
        duration = Duration.ofSeconds(60),
        renew = Duration.ofSeconds(30)
    ),

    /**
     * Node directory configuration
     */
    val nodeDirectoryConfig: InjectedWithConfig<NodeDirectory> = EtcdNodeDirectory.EtcdNodeDirectoryConfig(
        url = System.getenv("ETCD_SERVER") ?: "http://localhost:2379",
        expiration = leaseExpiration
    )
) {
    interface InjectedWithConfig<T> {
        val instanceType: Class<out T>
    }
}
