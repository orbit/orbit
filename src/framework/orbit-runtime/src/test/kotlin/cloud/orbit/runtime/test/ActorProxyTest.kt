/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.core.actor.ActorWithNoKey
import cloud.orbit.core.actor.getReference
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.runtime.util.StageBaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@NonConcrete
interface NonConcreteActor : ActorWithNoKey

interface ConcreteActor : ActorWithNoKey

class ActorProxyTest : StageBaseTest() {
    @Test
    fun `ensure concrete can be proxied`() {
        stage.actorProxyFactory.getReference<ConcreteActor>()
    }

    @Test
    fun `ensure non-concrete can not be proxied`() {
        assertThrows<IllegalArgumentException> {
            stage.actorProxyFactory.getReference<NonConcreteActor>()
        }
    }
}