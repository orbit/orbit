/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.stage

import cloud.orbit.common.concurrent.Pools
import cloud.orbit.common.util.RandomUtils
import cloud.orbit.core.net.ClusterName
import cloud.orbit.core.net.NodeIdentity
import cloud.orbit.runtime.net.NetConfig
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The configuration for an Orbit [Stage].
 */
data class StageConfig(
    /**
     * Network related configuration
     */
    val netConfig: NetConfig = NetConfig(),

    /**
     * The pool where CPU intensive tasks will run.
     */
    val cpuPool: CoroutineDispatcher = Pools.createFixedPool("orbit-cpu"),

    /**
     * The pool where IO intensive tasks will run.
     */
    val ioPool: CoroutineDispatcher = Pools.createCachedPool("orbit-io"),

    /**
     * The Orbit tick rate in milliseconds.
     */
    val tickRate: Long = 1000
)