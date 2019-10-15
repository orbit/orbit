/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.CoroutineDispatcher
import orbit.client.net.OrbitServiceLocator
import orbit.util.concurrent.jvm.Pools
import java.time.Duration

data class OrbitClientConfig(
    /**
     * The service locator for the Orbit cluster to connect to.
     */
    val serviceLocator: OrbitServiceLocator = OrbitServiceLocator("orbit://localhost:50056/default"),

    /**
     * The tick rate of the Orbit client.
     */
    val tickRate: Duration = Duration.ofSeconds(1),

    /**
     * The pool where Orbit client tasks will run.
     */
    val pool: CoroutineDispatcher = Pools.createFixedPool("orbit-client"),

    /**
     * The number of workers that can process a message concurrently.
     */
    val railCount: Int = 128,

    /**
     * The number of messages (inbound) that may be queued before new messages are rejected.
     */
    val bufferCount: Int = 10_000,

    /**
     * Packages to scan for addressables.
     *
     * If blank all packages will be scanned.
     */
    val packages: List<String> = listOf()

)