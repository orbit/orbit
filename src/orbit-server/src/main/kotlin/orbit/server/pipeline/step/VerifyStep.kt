/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.mesh.ClusterManager
import orbit.server.mesh.MANAGEMENT_NAMESPACE
import orbit.server.pipeline.PipelineContext
import orbit.shared.exception.InvalidNodeId
import orbit.shared.net.Message

class VerifyStep(
    private val clusterManager: ClusterManager
) : PipelineStep {
    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        val newSource = msg.source.let { src ->
            // If there is no source we set it
            // We also can't trust clients so we check the namespace
            if (src == null || context.metadata.connectedNamespace != MANAGEMENT_NAMESPACE) {
                context.metadata.connectedNode
            } else {
                src
            }
        }

        if (clusterManager.getNode(newSource) == null) {
            throw InvalidNodeId(newSource)
        }

        val newMsg = msg.copy(
            source = newSource
        )

        context.next(newMsg)
    }
}