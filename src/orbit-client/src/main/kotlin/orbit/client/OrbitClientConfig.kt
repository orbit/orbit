/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.CoroutineDispatcher
import orbit.client.addressable.AddressableConstructor
import orbit.client.addressable.DefaultAddressableConstructor
import orbit.client.mesh.NodeLeaseRenewalFailedHandler
import orbit.client.mesh.RestartOnNodeRenewalFailure
import orbit.client.net.OrbitServiceLocator
import orbit.util.concurrent.Pools
import orbit.util.di.ExternallyConfigured
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
    val packages: List<String> = listOf(),

    /**
     * The default timeout for messages.
     */
    val messageTimeout: Duration = Duration.ofSeconds(10),

    /**
     * The default timeout for deactivation.
     */
    val deactivationTimeout: Duration = Duration.ofSeconds(10),

    /**
     * The default TTL for addressables.
     */
    val addressableTTL: Duration = Duration.ofMinutes(10),

    /**
     * The system to use to construct addressables.
     */
    val addressableConstructor: ExternallyConfigured<AddressableConstructor> = DefaultAddressableConstructor.DefaultAddressableConstructorSingleton,

    /**
     * The amount of times the gRPC network layer will retry.
     */
    val networkRetryAttempts: Int = 5,

    /**
     * The amount of time Orbit should wait for the initial join cluster to succeed before failing.
     */
    val joinClusterTimeout: Duration = Duration.ofSeconds(30),

    /**
     * How to handle node lease renewal failure
     */
    val nodeLeaseRenewalFailedHandler: ExternallyConfigured<NodeLeaseRenewalFailedHandler> = RestartOnNodeRenewalFailure.RestartOnNodeRenewalFailureSingleton
)