/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import orbit.server.pipeline.step.AuthStep
import orbit.server.pipeline.step.IdentityStep
import orbit.server.pipeline.step.PipelineStep
import orbit.server.pipeline.step.PlacementStep
import orbit.server.pipeline.step.RetryStep
import orbit.server.pipeline.step.RoutingStep
import orbit.server.pipeline.step.TransportStep
import orbit.server.pipeline.step.VerifyStep

class PipelineSteps(
    identityStep: IdentityStep,
    placementStep: PlacementStep,
    verifyStep: VerifyStep,
    retryStep: RetryStep,
    routingStep: RoutingStep,
    authStep: AuthStep,
    transportStep: TransportStep
) {
    val steps: Array<PipelineStep> = arrayOf(
        identityStep,
        routingStep,
        retryStep,
                //- check cluster manager for node, pause if null and reintroduce to pipeline (log backoff)
        placementStep,
        verifyStep,
        authStep,
        transportStep
    )
}
// inbound is up, outbound is down