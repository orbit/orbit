/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import orbit.shared.addressable.Key

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

interface IdActor : ActorWithStringKey {
    fun getId(): Deferred<String>
}

class IdActorImpl : AbstractActor(), IdActor {
    override fun getId(): Deferred<String> {
        val stringKey = context.reference.key as Key.StringKey
        return CompletableDeferred(stringKey.key)
    }
}