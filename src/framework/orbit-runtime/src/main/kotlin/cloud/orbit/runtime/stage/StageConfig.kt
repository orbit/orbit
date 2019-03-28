/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.stage

import cloud.orbit.common.concurrent.Pools
import cloud.orbit.common.util.RandomUtils
import cloud.orbit.core.net.ClusterName
import cloud.orbit.core.net.NodeIdentity
import cloud.orbit.core.net.NodeMode
import cloud.orbit.runtime.cluster.ClusterConfig
import cloud.orbit.runtime.pipeline.steps.*
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The configuration for an Orbit [Stage].
 */
data class StageConfig(
    /**
     * The [ClusterName] of the Orbit cluster.
     *
     * This value determines which nodes may communicate with one another.
     */
    val clusterName: ClusterName = ClusterName("orbit-cluster"),

    /**
     * The [NodeIdentity] of this Orbit node.
     *
     * This value must be unique.
     */
    val nodeIdentity: NodeIdentity = NodeIdentity(RandomUtils.secureRandomString()),

    /**
     * The [NodeMode] of this Orbit node.
     */
    val nodeMode: NodeMode = NodeMode.SERVER,

    /**
     * A [ClusterConfig] that determines which components will be used for stage clustering.
     */
    val clusterConfig: ClusterConfig = ClusterConfig(),

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
    val tickRate: Long = 1000,

    /**
     * The default timeout for messages.
     */
    val messageTimeoutMillis: Long = 3_000,

    /**
     * The default time to live for deactivatable addressables.
     */
    val timeToLiveMillis: Long = 600_000,

    /**
     * Packages to scan for addressables.
     *
     * If blank all packages will be scanned.
     */
    val packages: List<String> = listOf(),

    /**
     * The number of workers that can process a content concurrently.
     */
    val pipelineRailCount: Int = 128,

    /**
     * The number of messages (either inbound or outbound) that may be queued before new messages are rejected.
     */
    val pipelineBufferCount: Int = 100_000
)