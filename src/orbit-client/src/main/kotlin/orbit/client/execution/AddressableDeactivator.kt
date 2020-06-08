/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import orbit.util.di.ExternallyConfigured
import orbit.util.time.highResolutionTicker

typealias Deactivator = suspend (handle: Deactivatable) -> Unit

@OptIn(FlowPreview::class)
abstract class AddressableDeactivator() {
    protected val logger = KotlinLogging.logger { }

    abstract suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator)

    @OptIn(ExperimentalCoroutinesApi::class)
    protected suspend fun deactivateItems(
        addressables: List<Deactivatable>,
        concurrency: Int,
        deactivationsPerSecond: Long,
        deactivate: Deactivator
    ) {
        val ticker = highResolutionTicker(deactivationsPerSecond)

        addressables.asFlow().onEach { ticker.receive() }
            .flatMapMerge(concurrency) { a ->
                flow { emit(deactivate(a)) }
            }.toList()
    }

    class Concurrent(private val config: Config) : AddressableDeactivator() {
        data class Config(val deactivationConcurrency: Int) :
            ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = Concurrent::class.java
        }

        override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
            deactivateItems(addressables, config.deactivationConcurrency, Long.MAX_VALUE, deactivate)
        }
    }

    class RateLimited(private val config: Config) : AddressableDeactivator() {
        data class Config(val deactivationsPerSecond: Long) :
            ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = RateLimited::class.java
        }

        override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
            deactivateItems(addressables, Int.MAX_VALUE, config.deactivationsPerSecond, deactivate)
        }
    }

    class TimeSpan(private val config: Config) : AddressableDeactivator() {
        data class Config(val deactivationTimeMilliSeconds: Long) :
            ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = TimeSpan::class.java
        }

        override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
            val deactivationsPerSecond = addressables.count() * 1000 / config.deactivationTimeMilliSeconds

            deactivateItems(addressables, Int.MAX_VALUE, deactivationsPerSecond, deactivate)
        }
    }

    class Instant : AddressableDeactivator() {
        class Config : ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = Instant::class.java
        }

        override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
            deactivateItems(addressables, Int.MAX_VALUE, Long.MAX_VALUE, deactivate)
        }
    }
}
