/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.common.exception.ResponseTimeoutException
import cloud.orbit.common.time.TimeMs
import cloud.orbit.core.actor.ActorWithNoKey
import cloud.orbit.core.actor.getReference
import cloud.orbit.core.annotation.OnActivate
import cloud.orbit.core.annotation.OnDeactivate
import cloud.orbit.runtime.util.StageBaseTest
import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

var wasDeactivated: Boolean = false

interface BasicTestActorInterface : ActorWithNoKey {
    fun echo(msg: String): Deferred<String>
    fun waitFor(delayMs: TimeMs): Deferred<Unit>
    fun incrementCountAndGet(): Deferred<Int>
    fun throwIllegalArgumentException(msg: String): Deferred<String>
    fun getWasActivated(): Deferred<Boolean>
}

class BasicTestActorImpl : BasicTestActorInterface {
    var callCount = 0
    var wasActivated = false

    @OnActivate
    fun onActivate(): Deferred<Unit> {
        wasActivated = true
        return CompletableDeferred(Unit)
    }

    @OnDeactivate
    fun onDeactivate(): Deferred<Unit> {
        wasDeactivated = true
        return CompletableDeferred(Unit)
    }

    override fun getWasActivated(): Deferred<Boolean> {
        return CompletableDeferred(wasActivated)
    }

    override fun echo(msg: String): Deferred<String> {
        return CompletableDeferred(msg)
    }

    override fun waitFor(delayMs: TimeMs): Deferred<Unit> {
        return GlobalScope.async {
            delay(delayMs)
        }
    }

    override fun incrementCountAndGet(): Deferred<Int> {
        return CompletableDeferred(++callCount)
    }

    override fun throwIllegalArgumentException(msg: String): Deferred<String> {
        throw IllegalArgumentException(msg)
    }
}

class BasicActorTest : StageBaseTest() {
    @Test
    fun `ensure onActivate runs`() {
        val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
        val result = runBlocking {
            actor.getWasActivated().await()
        }
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `ensure onDeactivate runs`() {
        val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
        runBlocking {
            wasDeactivated = false
            actor.incrementCountAndGet().await()
            stage.clock.advanceTime(stageConfig.timeToLiveMillis * 2) // Make actor eligible for deactivation
            delay(stageConfig.tickRate * 2) // Wait twice the tick so the deactivation should have happened
            assertThat(wasDeactivated).isEqualTo(true)
        }
    }

    @Test
    fun `ensure basic echo has expected result`() {
        val echoMsg = "Hello Orbit!"
        val echo = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
        val result = runBlocking {
            echo.echo(echoMsg).await()
        }
        assertThat(result).isEqualTo(echoMsg)
    }

    @Test
    fun `ensure basic delay causes timeout`() {
        val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
        assertThatThrownBy {
            runBlocking {
                actor.waitFor(stageConfig.messageTimeoutMillis + 100).await()
            }
        }.isInstanceOf(ResponseTimeoutException::class.java)
    }

    @Test
    fun `ensure only one actor instance`() {
        val actor1 = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
        val actor2 = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
        runBlocking {
            val call1 = actor1.incrementCountAndGet().await()
            val call2 = actor2.incrementCountAndGet().await()
            assertThat(call2).isGreaterThan(call1)
        }
    }

    @Test
    fun `ensure actor deactivates`() {
        val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
        runBlocking {
            val call1 = actor.incrementCountAndGet().await()
            stage.clock.advanceTime(stageConfig.timeToLiveMillis * 2) // Make actor eligible for deactivation
            delay(stageConfig.tickRate * 2) // Wait twice the tick so the deactivation should have happened
            val call2 = actor.incrementCountAndGet().await()
            assertThat(call2).isLessThanOrEqualTo(call1)
        }
    }

    @Test
    fun `ensure exception propagated`() {
        val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()

        assertThatThrownBy {
            runBlocking {
                actor.throwIllegalArgumentException("Faileroony").await()
            }
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("Faileroony")
    }
}