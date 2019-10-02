/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import orbit.server.pipeline.step.PipelineStep
import orbit.server.pipeline.step.BlankPipelineStep

class PipelineSteps(
    blankPipelineStep: BlankPipelineStep
) {
    val steps: Array<PipelineStep> = arrayOf(
        blankPipelineStep
    )
}