/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import orbit.util.di.ExternallyConfigured

typealias Deactivator = suspend (handle: Deactivatable) -> Unit

abstract class AddressableDeactivator() {
    protected val logger = KotlinLogging.logger { }

    abstract suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator)

    protected suspend fun deactivateItems(
        addressables: List<Deactivatable>,
        concurrency: Int,
        deactivationsPerSecond: Long,
        deactivate: Deactivator
    ) {
        val tickRate = 1000 / deactivationsPerSecond

        println("Starting ${addressables.count()} item deactivations at one per ${tickRate}ms")

        val ticker = ticker(tickRate)

        addressables.asFlow().onEach { ticker.receive() }
            .flatMapMerge(concurrency) { a ->
                flow { emit(deactivate(a)) }
            }.toList()
    }

    @OptIn(FlowPreview::class)
    open class Concurrent(private val config: Config) : AddressableDeactivator() {
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
            println("Deactivate ${addressables.count()} addressables in ${config.deactivationTimeMilliSeconds}ms")
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
