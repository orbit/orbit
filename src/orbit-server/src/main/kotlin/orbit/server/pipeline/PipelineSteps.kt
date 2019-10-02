/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import orbit.server.pipeline.step.EchoStep
import orbit.server.pipeline.step.IdentityStep
import orbit.server.pipeline.step.PipelineStep
import orbit.server.pipeline.step.TransportStep
import orbit.server.pipeline.step.VerifyStep

class PipelineSteps(
    identityStep: IdentityStep,
    echoStep: EchoStep,
    verifyStep: VerifyStep,
    transportStep: TransportStep
) {
    val steps: Array<PipelineStep> = arrayOf(
        identityStep,
        echoStep,
        verifyStep,
        transportStep
    )
}