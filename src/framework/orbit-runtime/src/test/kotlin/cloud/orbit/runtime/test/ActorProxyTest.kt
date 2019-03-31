/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.core.actor.ActorWithNoKey
import cloud.orbit.core.actor.createProxy
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.runtime.util.StageBaseTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@NonConcrete
interface NonConcreteActor : ActorWithNoKey

interface ConcreteActor : ActorWithNoKey

class ActorProxyTest : StageBaseTest() {
    @Test
    fun `ensure concrete can be proxied`() {
        val proxy = stage.actorProxyFactory.createProxy<ConcreteActor>()
        assertThat(proxy).isInstanceOf(ConcreteActor::class.java)
    }

    @Test
    fun `ensure non-concrete can not be proxied`() {
        val e = assertThatThrownBy {
            stage.actorProxyFactory.createProxy<NonConcreteActor>()
        }

        e.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("can not be directly addressed")
    }
}