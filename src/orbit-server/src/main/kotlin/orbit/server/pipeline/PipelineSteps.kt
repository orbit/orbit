/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import orbit.server.pipeline.steps.AddressablePipelineStep
import orbit.server.pipeline.steps.ErrorPipelineStep
import orbit.server.pipeline.steps.LeasePipelineStep
import orbit.server.pipeline.steps.PipelineStep
import orbit.server.pipeline.steps.RoutingPipelineStep

internal class PipelineSteps(
    errorPipelineStep: ErrorPipelineStep,
    leasePipelineStep: LeasePipelineStep,
    routingPipelineStep: RoutingPipelineStep,
    addressablePipelineStep: AddressablePipelineStep
) {
    val steps: Array<PipelineStep> = arrayOf(
        routingPipelineStep,
        addressablePipelineStep,
        leasePipelineStep,
        errorPipelineStep
    )
}