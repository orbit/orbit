/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline.step

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import orbit.server.OrbitServerConfig
import orbit.server.pipeline.PipelineContext
import orbit.server.router.Router
import orbit.server.service.Meters
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget

class RoutingStep(
    private val router: Router,
    private val config: OrbitServerConfig
) : PipelineStep {
    private val retryAttempts: Counter
    private val retryErrors: Counter

    init {
        retryAttempts = Metrics.counter(Meters.Names.RetryAttempts)
        retryErrors = Metrics.counter(Meters.Names.RetryErrors)
    }

    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        checkNotNull(msg.target) { "Target may not be null for outbound messages. $msg" }
        val route = when (val target = msg.target) {
            is MessageTarget.Unicast -> {
                router.findRoute(target.targetNode)
            }
            is MessageTarget.RoutedUnicast -> {
                target.route.pop().let {
                    router.findRoute(
                        it.nodeId,
                        it.route
                    )
                }
            }
            else -> null
        }

        checkNotNull(route) { "Could not find route for $msg" }
        if (!route.isValid()) {
            retry(context, msg)
            return
        }

        val newMsg = msg.copy(
            target = MessageTarget.RoutedUnicast(route)
        )

        context.next(newMsg)
    }

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        // If a message gets here it should be resolved and ready to send out again
        checkNotNull(msg.target) { "Node target was not resolved" }
        context.pushNew(msg)
    }

    fun retry(context: PipelineContext, msg: Message) {
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
    }
}