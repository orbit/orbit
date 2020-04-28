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
import orbit.util.concurrent.Pools
import orbit.util.di.ComponentContainerRoot
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import java.time.Duration

data class OrbitClientConfig(
    /**
     * The gRPC endpoint where the Orbit cluster is located.
     */
    val grpcEndpoint: String = "dns:///localhost:50056/",

    /**
     * The namespace to use when connecting to the Orbit cluster.
     */
    val namespace: String = "default",

    /**
     * Client's application clock
     */
    val clock: Clock = Clock(),

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
     * The number of concurrent addressable deactivations during shutdown draining
     */
    val deactivationConcurrency: Int = 10,

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
     * The amount of time Orbit should wait to leave the cluster to succeed before failing.
     */
    val leaveClusterTimeout: Duration = Duration.ofSeconds(30),

    /**
     * How to handle node lease renewal failure
     */
    val nodeLeaseRenewalFailedHandler: ExternallyConfigured<NodeLeaseRenewalFailedHandler> = RestartOnNodeRenewalFailure.RestartOnNodeRenewalFailureSingleton,

    /**
     * Rethrow platform specific exceptions. Should only be used when all clients are using the same SDK.
     */
    val platformExceptions: Boolean = false,

    /**
     * Optional hook to update container registrations after initialization
     */
    val containerOverrides: ComponentContainerRoot.() -> Unit = { }
) {
}