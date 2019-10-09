/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import kotlinx.coroutines.CoroutineDispatcher
import orbit.server.mesh.AddressableDirectory
import orbit.server.mesh.LeaseDuration
import orbit.server.mesh.NodeDirectory
import orbit.server.mesh.local.LocalAddressableDirectory
import orbit.server.mesh.local.LocalNodeDirectory
import orbit.shared.net.PortBinding
import orbit.util.concurrent.jvm.Pools
import orbit.util.di.jvm.ExternallyConfigured
import java.time.Duration

data class OrbitServerConfig(
    val serverPort: PortBinding = PortBinding(
        host = System.getenv("ORBIT_HOST") ?: "0.0.0.0",
        port = System.getenv("ORBIT_PORT")?.toInt(10) ?: 50056
    ),
    /**
     * The Orbit tick rate.
     */
    val tickRate: Duration = Duration.ofSeconds(1),

    /**
     * The number of workers that can process a message concurrently.
     */
    val pipelineRailCount: Int = 128,

    /**
     * The number of messages (either inbound or outbound) that may be queued before new messages are rejected.
     */
    val pipelineBufferCount: Int = 10_000,


    /**
     * Expiration times for node leases
     */
    val nodeLeaseDuration: LeaseDuration = LeaseDuration(
        expiresIn = Duration.ofSeconds(30),
        renewIn = Duration.ofSeconds(5)
    ),

    /**
     * Expiration times for node leases
     */
    val addressableLeaseDuration: LeaseDuration = LeaseDuration(
        expiresIn = Duration.ofMinutes(10),
        renewIn = Duration.ofMinutes(5)
    ),

    /**
     * The pool where CPU intensive tasks will run.
     */
    val cpuPool: CoroutineDispatcher = Pools.createFixedPool("orbit-cpu"),

    /**
     * The pool where IO intensive tasks will run.
     */
    val ioPool: CoroutineDispatcher = Pools.createCachedPool("orbit-io"),

    /**
     * Prevents the JVM shutting down when the main thread exits.
     */
    val acquireShutdownLatch: Boolean = true,

    /**
     * The node directory to use.
     */
    val nodeDirectory: ExternallyConfigured<NodeDirectory> = LocalNodeDirectory.LocalNodeDirectoryConfig,

    /**
     * The addressable directory to use
     */
    val addressableDirectory: ExternallyConfigured<AddressableDirectory> = LocalAddressableDirectory.LocalAddressableDirectoryConfig

)