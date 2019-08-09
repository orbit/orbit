/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Server
import io.grpc.ServerBuilder
import orbit.common.logging.logger
import orbit.server.OrbitConfig
import orbit.server.demo.GreeterImpl

class GrpcEndpoint(private val config: OrbitConfig) {
    private lateinit var server: Server

    private val logger by logger()

    fun start() {
        logger.info("Starting gRPC Endpoint on port ${config.port}...")

        server = ServerBuilder.forPort(config.port)
            .addService(GreeterImpl())
            .build()
            .start()

        logger.info("gRPC Endpoint started on port ${config.port}.")

        // TODO: We need to handle threading correctly and not block here
        server.awaitTermination()
    }
}