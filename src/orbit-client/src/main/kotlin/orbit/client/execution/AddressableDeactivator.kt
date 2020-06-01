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
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import orbit.client.execution.AddressableDeactivator.Concurrent.Config
import orbit.util.di.ExternallyConfigured

typealias Deactivator = suspend (handle: Deactivatable) -> Unit

abstract class AddressableDeactivator() {
    private val logger = KotlinLogging.logger { }

    abstract suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator)

    @OptIn(FlowPreview::class)
    open class Concurrent(private val config: Config) : AddressableDeactivator() {
        data class Config(val deactivationConcurrency: Int) :
            ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = Concurrent::class.java
        }

        override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
            addressables.asFlow().flatMapMerge(concurrency = config.deactivationConcurrency) { addressable ->
                flow { emit(deactivate(addressable)) }
            }.toList()
        }
    }

    class RateLimited(private val config: Config) : AddressableDeactivator() {
        data class Config(val deactivationsPerSecond: Long) :
            ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = RateLimited::class.java
        }

        override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
            val tickRate = 1000 / config.deactivationsPerSecond

            println("Starting ${addressables.count()} item deactivations at one per ${tickRate}ms. ")

            val ticker = ticker(tickRate)

            addressables.forEach { a ->
                ticker.receive()
                deactivate(a)
            }
        }
    }

    class TimeSpan(private val config: Config) : AddressableDeactivator() {
        data class Config(val deactivationTimeMilliSeconds: Long) :
            ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = TimeSpan::class.java
        }

        override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
            val tickRate = config.deactivationTimeMilliSeconds / addressables.count()

            println("Starting ${addressables.count()} item deactivations at one per ${tickRate}ms")

            val ticker = ticker(tickRate)

            addressables.forEach { a ->
                ticker.receive()
                deactivate(a)
            }
        }
    }

    class Instant : Concurrent(Config(Int.MAX_VALUE)) {
        class Config :
            ExternallyConfigured<AddressableDeactivator> {
            override val instanceType: Class<out AddressableDeactivator> = Instant::class.java
        }
    }
}