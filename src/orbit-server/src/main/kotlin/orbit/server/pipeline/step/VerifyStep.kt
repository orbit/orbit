/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.mesh.ClusterManager
import orbit.server.pipeline.PipelineContext
import orbit.shared.exception.InvalidNodeId
import orbit.shared.net.Message

class VerifyStep(
    private val clusterManager: ClusterManager
) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        val source = checkNotNull(msg.source) { "Message source must not be null" }

        if (clusterManager.getNode(source) == null) {
            throw InvalidNodeId(source)
        }

        context.nextInbound(msg)
    }
}