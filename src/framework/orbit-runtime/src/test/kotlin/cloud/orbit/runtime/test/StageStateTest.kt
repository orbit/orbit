/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.core.actor.getReference
import cloud.orbit.runtime.stage.Stage
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException

class StageStateTest {
    @Test
    fun `ensure fails when not started`() {
        assertThatThrownBy {
            runBlocking {
                val config = StageConfig(
                    packages = listOf("cloud.orbit.runtime.test")
                )
                val stage = Stage(config)
                val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()

                actor.incrementCountAndGet().await()

            }
        }.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("pipeline")
    }

    @Test
    fun `ensure fails after stopped`() {
        assertThatThrownBy {
            runBlocking {
                val config = StageConfig(
                    packages = listOf("cloud.orbit.runtime.test")
                )
                val stage = Stage(config)
                stage.start().await()
                val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()
                stage.stop().await()
                actor.incrementCountAndGet().await()

            }
        }.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("pipeline")
    }

    @Test
    fun `test start stop start`() {
        runBlocking {
            val config = StageConfig(
                packages = listOf("cloud.orbit.runtime.test")
            )
            val stage = Stage(config)
            val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()

            stage.start().await()
            actor.incrementCountAndGet().await()

            stage.stop().await()
            stage.start().await()
            actor.incrementCountAndGet().await() }
    }

    @Test
    fun `ensure stop deactivates addressables`() {
        runBlocking {
            val config = StageConfig(
                packages = listOf("cloud.orbit.runtime.test")
            )
            val stage = Stage(config)
            val actor = stage.actorProxyFactory.getReference<BasicTestActorInterface>()

            stage.start().await()
            actor.incrementCountAndGet().await()

            wasDeactivated = false

            stage.stop().await()

            assertThat(wasDeactivated).isTrue()
        }
    }
}