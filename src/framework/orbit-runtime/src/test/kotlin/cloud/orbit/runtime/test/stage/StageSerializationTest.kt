/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test.stage

import cloud.orbit.common.util.RandomUtils
import cloud.orbit.core.actor.createProxy
import cloud.orbit.runtime.serialization.kryo.DEFAULT_KRYO_BUFFER_SIZE
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class StageSerializationTest : StageBaseTest() {
    override fun setupStage(stageConfig: StageConfig): StageConfig {
        return stageConfig.copy(
            allowLoopback = false
        )
    }

    @Test
    fun `ensure too large message succeeds`() {
        val echoMsg = RandomUtils.pseudoRandomString(DEFAULT_KRYO_BUFFER_SIZE * 2)
        val echo = stage.actorProxyFactory.createProxy<BasicTestActorInterface>()
        val result = runBlocking {
            echo.echo(echoMsg).await()
        }
        Assertions.assertThat(result).isEqualTo(echoMsg)
    }
}