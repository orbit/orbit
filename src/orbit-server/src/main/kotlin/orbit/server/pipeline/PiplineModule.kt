/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import orbit.server.pipeline.steps.BlankPipelineStep
import org.kodein.di.Kodein
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.singleton

val pipelineModule = Kodein.Module(name = "Pipeline") {
    bind() from singleton { Pipeline(instance(), instance(), instance()) }
    bind() from singleton { BlankPipelineStep() }
}