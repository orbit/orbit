/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

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