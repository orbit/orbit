/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import orbit.server.mesh.ClusterManager
import orbit.server.pipeline.PipelineContext
import orbit.shared.net.Message
import orbit.shared.net.MessageTarget

class RetryStep(private val clusterManager: ClusterManager) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        val target = when (val target = msg.target) {
            is MessageTarget.Unicast -> {
                target.targetNode
            }
            is MessageTarget.RoutedUnicast -> {
                target.route.destinationNode
            }
            else -> null
        }

        if (target != null) {
            if (clusterManager.getNode(target) == null) {
                context.pushNew(
                    msg.copy(
                        attempts = msg.attempts + 1
                    )
                )
                return
            }
        }

        context.next(msg)
    }
}