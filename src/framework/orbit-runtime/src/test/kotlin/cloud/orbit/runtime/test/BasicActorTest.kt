/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.core.actor.ActorWithNoKey
import cloud.orbit.core.actor.getReference
import cloud.orbit.runtime.util.StageBaseTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test

interface BasicEcho : ActorWithNoKey {
    fun echo(msg: String): CompletableDeferred<String>
}

class BasicEchoActor : BasicEcho {
    override fun echo(msg: String): CompletableDeferred<String> {
        return CompletableDeferred(msg)
    }
}

class BasicActorTest : StageBaseTest() {
    // TODO: Enable test, not enough is done yet for this to pass
    // @Test
    fun `ensure basic echo has expected result`() {
        val echoMsg = "Hello Orbit!"
        val echo = stage.actorProxyFactory.getReference<BasicEcho>()
        val result = runBlocking {
            echo.echo(echoMsg).await()
        }
        assertThat(result).isEqualTo(echoMsg)

    }

}