/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import orbit.server.OrbitServerConfig
import orbit.server.mesh.ClusterManager
import orbit.server.pipeline.PipelineContext
import orbit.server.service.Meters
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget

class RetryStep(
    private val clusterManager: ClusterManager,
    private val config: OrbitServerConfig
) : PipelineStep {
    private val retryAttempts: Counter
    private val retryErrors: Counter

    init {
        retryAttempts = Metrics.counter(Meters.Names.RetryAttempts)
        retryErrors = Metrics.counter(Meters.Names.RetryErrors)
    }

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
                    if (msg.attempts < config.messageRetryAttempts) {
                        retryAttempts.increment()
                        msg.copy(
                            attempts = msg.attempts + 1
                        )
                    } else {
                        retryErrors.increment()
                        msg.copy(
                            content = MessageContent.Error("Failed to deliver message after ${msg.attempts} attempts"),
                            target = MessageTarget.Unicast(msg.source!!)
                        )
                    }
                )
                return
            }
        }

        context.next(msg)
    }
}