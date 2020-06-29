/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import orbit.client.addressable.DeactivationReason
import orbit.client.addressable.OnActivate
import orbit.client.addressable.OnDeactivate
import orbit.shared.addressable.Key
import orbit.shared.mesh.NodeId
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

object TrackingGlobals {
    fun reset() {
        deactivateTestCounts.set(0)
        concurrentDeactivations.set(0)
        maxConcurrentDeactivations.set(0)
        deactivatedActors.clear()
    }

    fun startDeactivate() {
        lock.withLock {
            concurrentDeactivations.incrementAndGet()
        }
    }

    fun endDeactivate() {
        lock.withLock {
            deactivateTestCounts.incrementAndGet()
            maxConcurrentDeactivations.getAndAccumulate(concurrentDeactivations.get()) { a, b -> Math.max(a, b) }
            concurrentDeactivations.decrementAndGet()
        }
    }

    val lock: Lock = ReentrantLock()

    val deactivateTestCounts = AtomicInteger(0)
    val concurrentDeactivations = AtomicInteger(0)
    val maxConcurrentDeactivations = AtomicInteger(0)
    val deactivatedActors: MutableList<Key> = mutableListOf()
}

interface GreeterActor : ActorWithNoKey {
    fun greetAsync(name: String): Deferred<String>
}

class GreeterActorImpl : GreeterActor {
    override fun greetAsync(name: String): Deferred<String> =
        CompletableDeferred("Hello $name")
}

interface TimeoutActor : ActorWithNoKey {
    fun timeout(): Deferred<Unit>
}

class TimeoutActorImpl : TimeoutActor {
    override fun timeout() = CompletableDeferred<Unit>()
}

interface ActorWithNoImpl : ActorWithNoKey {
    fun greetAsync(name: String): Deferred<String>
}

interface ComplexDtoActor : ActorWithNoKey {
    data class ComplexDto(val blah: String)

    fun complexCall(dto: ComplexDto): Deferred<Unit>
}

class ComplexDtoActorImpl : ComplexDtoActor {
    override fun complexCall(dto: ComplexDtoActor.ComplexDto) = CompletableDeferred(Unit)
}

interface IncrementActor : ActorWithNoKey {
    fun increment(): Deferred<Long>
}

class IncrementActorImpl : IncrementActor {
    var counter = 1L

    override fun increment(): Deferred<Long> =
        CompletableDeferred(++counter)
}

class TestException(msg: String) : RuntimeException(msg)

interface ThrowingActor : ActorWithNoKey {
    fun doThrow(): Deferred<Long>
}

class ThrowingActorImpl : ThrowingActor {
    override fun doThrow(): Deferred<Long> {
        throw TestException("Threw")
    }
}

interface IdActor : ActorWithStringKey {
    fun getId(): Deferred<String>
}

class IdActorImpl : AbstractActor(), IdActor {
    override fun getId(): Deferred<String> {
        val stringKey = context.reference.key as Key.StringKey
        return CompletableDeferred(stringKey.key)
    }
}

data class ComplexNull(
    val greeting: String
)

interface NullActor : ActorWithNoKey {
    fun simpleNull(arg1: String, arg2: String?): Deferred<String>
    fun complexNull(arg1: String, arg2: ComplexNull?): Deferred<String>
}

class NullActorImpl : NullActor {
    override fun simpleNull(arg1: String, arg2: String?): Deferred<String> {
        return CompletableDeferred(arg1 + arg2)
    }

    override fun complexNull(arg1: String, arg2: ComplexNull?): Deferred<String> {
        return CompletableDeferred(arg1 + arg2?.greeting)
    }
}

interface ClientAwareActor : ActorWithStringKey {
    fun getClient(): Deferred<NodeId>
}

class ClientAwareActorImpl : AbstractActor(), ClientAwareActor {
    override fun getClient(): Deferred<NodeId> {
        return CompletableDeferred(context.client.nodeId!!)
    }

    @OnDeactivate
    fun onDeactivate(): Deferred<Unit> {
        println("Deactivating actor ${context.reference.key}")
        TrackingGlobals.deactivatedActors.add(context.reference.key)
        return CompletableDeferred(Unit)
    }
}

interface BasicOnDeactivate : ActorWithNoKey {
    fun greetAsync(name: String): Deferred<String>
}

class BasicOnDeactivateImpl : BasicOnDeactivate {
    override fun greetAsync(name: String): Deferred<String> =
        CompletableDeferred("Hello $name")

    @OnDeactivate
    fun onDeactivate(): Deferred<Unit> {
        TrackingGlobals.deactivateTestCounts.incrementAndGet()
        return CompletableDeferred(Unit)
    }
}

interface ArgumentOnDeactivate : ActorWithNoKey {
    fun greetAsync(name: String): Deferred<String>
}

class ArgumentOnDeactivateImpl : ArgumentOnDeactivate {
    override fun greetAsync(name: String): Deferred<String> =
        CompletableDeferred("Hello $name")

    @Suppress("UNUSED_PARAMETER")
    @OnDeactivate
    fun onDeactivate(deactivationReason: DeactivationReason): Deferred<Unit> {
        TrackingGlobals.deactivateTestCounts.incrementAndGet()
        return CompletableDeferred(Unit)
    }
}

interface KeyedDeactivatingActor : ActorWithInt32Key {
    fun ping(): Deferred<Unit>
}

class KeyedDeactivatingActorImpl : KeyedDeactivatingActor {
    override fun ping(): Deferred<Unit> = CompletableDeferred(Unit)

    @Suppress("UNUSED_PARAMETER")
    @OnDeactivate
    fun onDeactivate(deactivationReason: DeactivationReason): Deferred<Unit> {
        TrackingGlobals.deactivateTestCounts.incrementAndGet()
        return CompletableDeferred(Unit)
    }
}

interface SlowDeactivateActor : ActorWithInt32Key {
    fun ping(msg: String = ""): Deferred<String>
}

class SlowDeactivateActorImpl : SlowDeactivateActor {
    override fun ping(msg: String): Deferred<String> =
        CompletableDeferred(msg)

    @Suppress("UNUSED_PARAMETER")
    @OnDeactivate
    fun onDeactivate(deactivationReason: DeactivationReason): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()

        GlobalScope.launch {
            TrackingGlobals.startDeactivate()
            delay(Random.nextLong(50) + 50)
            TrackingGlobals.endDeactivate()
            deferred.complete(Unit)
        }

        return deferred
    }
}

interface SuspendingMethodActor : ActorWithStringKey {
    suspend fun ping(msg: String = ""): String
}

class SuspendingMethodActorImpl : SuspendingMethodActor {
    @OnActivate
    suspend fun onActivate() {
        delay(1)
        println("Activated")
    }

    @OnDeactivate
    suspend fun onDeactivate(deactivationReason: DeactivationReason) {
        delay(1)
        println("Deactivated: ${deactivationReason}")
        TrackingGlobals.endDeactivate()
    }

    override suspend fun ping(msg: String): String {
        delay(1)
        println("Ping: ${msg}")
        return msg
    }
}
