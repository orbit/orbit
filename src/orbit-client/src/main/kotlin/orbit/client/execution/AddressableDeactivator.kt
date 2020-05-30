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
import orbit.client.execution.ConcurrentDeactivator.Config
import orbit.util.di.ExternallyConfigured

typealias Deactivator = suspend (handle: Deactivatable) -> Unit

abstract class AddressableDeactivator() {
    private val logger = KotlinLogging.logger { }

    abstract suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator)
}

@OptIn(FlowPreview::class)
open class ConcurrentDeactivator(private val config: Config) : AddressableDeactivator() {
    data class Config(val deactivationConcurrency: Int) :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = ConcurrentDeactivator::class.java
    }

    override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
        addressables.asFlow().flatMapMerge(concurrency = config.deactivationConcurrency) { addressable ->
            flow { emit(deactivate(addressable)) }
        }.toList()
    }
}

class RateLimitedDeactivator(private val config: Config) : AddressableDeactivator() {
    data class Config(val deactivationsPerSecond: Int) :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = RateLimitedDeactivator::class.java
    }

    override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
        val tickRate = 1000L / config.deactivationsPerSecond

        println("Starting ${addressables.count()} item deactivations at ${tickRate}ms")

        val ticker = ticker(tickRate)

        addressables.forEach { a ->
            ticker.receive()
            deactivate(a)
        }
    }
}

class TimeSpanDeactivator(private val config: Config) : AddressableDeactivator() {
    data class Config(val deactivationTimeSeconds: Int) :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = TimeSpanDeactivator::class.java
    }

    override suspend fun deactivate(addressables: List<Deactivatable>, deactivate: Deactivator) {
        TODO("Not yet implemented")
    }
}

class InstantDeactivator : ConcurrentDeactivator(Config(Int.MAX_VALUE)) {
    class Config :
        ExternallyConfigured<AddressableDeactivator> {
        override val instanceType: Class<out AddressableDeactivator> = InstantDeactivator::class.java
    }
}