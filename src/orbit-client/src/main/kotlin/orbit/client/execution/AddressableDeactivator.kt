/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import orbit.util.di.ExternallyConfigured

internal typealias Deactivator = suspend (handle: ExecutionHandle) -> Unit

internal abstract class AddressableDeactivator() {
    private val logger = KotlinLogging.logger { }

    abstract suspend fun deactivate(addressables: List<ExecutionHandle>, deactivate: Deactivator)
}

internal class ConcurrentDeactivator(private val config: Config) : AddressableDeactivator() {

    data class Config(val deactivationConcurrency: Int) :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = ConcurrentDeactivator::class.java
    }

    override suspend fun deactivate(addressables: List<ExecutionHandle>, deactivate: Deactivator) {
        while (addressables.count() > 0) {
            addressables.asFlow().flatMapMerge(concurrency = config.deactivationConcurrency) { addressable ->
                flow { emit(deactivate(addressable)) }
            }.toList()
        }
    }
}

internal class RateLimitedDeactivator(private val config: Config) : AddressableDeactivator() {
    data class Config(val deactivationsPerSecond: Int) :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = RateLimitedDeactivator::class.java
    }

    override suspend fun deactivate(addressables: List<ExecutionHandle>, deactivate: Deactivator) {
        TODO("Not yet implemented")
    }
}

internal class TimeSpanDeactivator(private val config: Config) : AddressableDeactivator() {
    data class Config(val deactivationTimeSeconds: Int) :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = TimeSpanDeactivator::class.java
    }

    override suspend fun deactivate(addressables: List<ExecutionHandle>, deactivate: Deactivator) {
        TODO("Not yet implemented")
    }
}

internal class InstantDeactivator(private val config: Config) : AddressableDeactivator() {
    class Config :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = InstantDeactivator::class.java
    }

    override suspend fun deactivate(addressables: List<ExecutionHandle>, deactivate: Deactivator) {
        while (addressables.count() > 0) {
            addressables.forEach { addressable ->
                deactivate(addressable)
            }
        }
    }

}