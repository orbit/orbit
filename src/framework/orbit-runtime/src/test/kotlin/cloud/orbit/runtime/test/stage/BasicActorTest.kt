/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test.stage

import cloud.orbit.common.exception.ResponseTimeoutException
import cloud.orbit.common.time.TimeMs
import cloud.orbit.core.actor.ActorWithNoKey
import cloud.orbit.core.actor.createProxy
import cloud.orbit.core.annotation.OnActivate
import cloud.orbit.core.annotation.OnDeactivate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

var wasDeactivated: Boolean = false

interface BasicTestActorInterface : ActorWithNoKey {
    fun waitFor(delayMs: TimeMs): Deferred<Unit>
    fun echo(msg: String): Deferred<String>
    fun incrementCountAndGet(): Deferred<Int>
    fun getWasActivated(): Deferred<Boolean>
    fun throwIllegalArgumentException(msg: String): Deferred<String>


}

@Suppress("UNUSED")
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

    override fun waitFor(delayMs: TimeMs) = GlobalScope.async {
        delay(delayMs)
    }

    override fun echo(msg: String) = CompletableDeferred(msg)
    override fun incrementCountAndGet() = CompletableDeferred(++callCount)
    override fun getWasActivated() = CompletableDeferred(wasActivated)
    override fun throwIllegalArgumentException(msg: String): Deferred<String> = throw IllegalArgumentException(msg)
}

class BasicActorTest : BaseStageTest() {
    @Test
    fun `ensure onActivate runs`() {
        val actor = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
        val result = runBlocking {
            actor.getWasActivated().await()
        }
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `ensure onDeactivate runs`() {
        val actor = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
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
        val echoMsg = "Chevron one encoded..."
        val echo = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
        val result = runBlocking {
            echo.echo(echoMsg).await()
        }
        assertThat(result).isEqualTo(echoMsg)
    }

    @Test
    fun `ensure basic delay causes timeout`() {
        val actor = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
        assertThatThrownBy {
            runBlocking {
                actor.waitFor(stageConfig.messageTimeoutMillis + (stageConfig.tickRate * 2)).await()
            }
        }.isInstanceOf(ResponseTimeoutException::class.java)
    }

    @Test
    fun `ensure only one actor instance`() {
        val actor1 = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
        val actor2 = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
        runBlocking {
            val call1 = actor1.incrementCountAndGet().await()
            val call2 = actor2.incrementCountAndGet().await()
            assertThat(call2).isGreaterThan(call1)
        }
    }

    @Test
    fun `ensure actor deactivates`() {
        val actor = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
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
        val errMsg = "Chevron seven will not lock."
        val actor = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()

        assertThatThrownBy {
            runBlocking {
                actor.throwIllegalArgumentException(errMsg).await()
            }
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessage(errMsg)
    }
}