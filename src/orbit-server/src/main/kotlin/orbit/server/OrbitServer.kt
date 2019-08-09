/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import orbit.common.logging.logger
import orbit.server.net.GrpcEndpoint
import org.kodein.di.Kodein
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.singleton

class OrbitServer(private val config: OrbitConfig) {
    private val logger by logger()

    private val kodein = Kodein {
        bind<OrbitConfig>() with singleton { config }
        bind<GrpcEndpoint>() with singleton { GrpcEndpoint(instance()) }
    }

    fun start() {
        logger.info("Starting Orbit...")

        val endpoint: GrpcEndpoint by kodein.instance()

        endpoint.start()

        logger.info("Orbit started.")
    }
}