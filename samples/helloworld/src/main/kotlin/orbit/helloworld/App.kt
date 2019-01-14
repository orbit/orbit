/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld

import cloud.orbit.core.actor.AbstractActor
import cloud.orbit.core.actor.ActorWithStringKey
import cloud.orbit.core.actor.getReference
import cloud.orbit.runtime.Stage
import cloud.orbit.runtime.config.StageConfig
import kotlinx.coroutines.runBlocking

interface Hello : ActorWithStringKey
class HelloActor : Hello, AbstractActor()

fun main(args: Array<String>) {
    val stageConfig = StageConfig()
    val stage = Stage(stageConfig)

    runBlocking {
        stage.start().await()
        //val hello = stage.actorProxyFactory.getReference<Hello>("test")
    }
}