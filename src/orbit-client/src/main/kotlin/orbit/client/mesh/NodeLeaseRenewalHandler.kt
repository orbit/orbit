/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.mesh

import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.client.OrbitClient
import orbit.util.concurrent.SupervisorScope
import orbit.util.di.ExternallyConfigured

interface NodeLeaseRenewalFailedHandler {
    fun onLeaseRenewalFailed()
}

class RestartOnNodeRenewalFailure(private val orbitClient: OrbitClient, private val supervisorScope: SupervisorScope) :
    NodeLeaseRenewalFailedHandler {
    val logger = KotlinLogging.logger { }

    object RestartOnNodeRenewalFailureSingleton : ExternallyConfigured<NodeLeaseRenewalFailedHandler> {
        override val instanceType = RestartOnNodeRenewalFailure::class.java
    }

    override fun onLeaseRenewalFailed() {
        supervisorScope.launch {
            logger.info { "Beginning Orbit restart..." }
            orbitClient.stop()
            orbitClient.start()
            logger.info { "Restart complete" }

        }
    }
}

class NodeLeaseRenewalFailed(msg: String) : Throwable(msg)