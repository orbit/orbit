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
    val pool: CoroutineDispatcher = Pools.createFixedPool("orbit-client")
)