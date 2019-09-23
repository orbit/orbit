/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import io.grpc.Server
import io.grpc.ServerBuilder
import orbit.common.logging.logger
import orbit.server.OrbitServerConfig

internal class GrpcEndpoint(
    private val config: OrbitServerConfig,
    private val connections: Connections,
    private val nodeLeases: NodeLeases
) {
    private lateinit var server: Server

    private val logger by logger()

    fun start() {
        logger.info("Starting gRPC Endpoint on port ${config.grpcPort}...")

        server = ServerBuilder.forPort(config.grpcPort)
            .addService(connections)
            .addService(nodeLeases)
            .intercept(NodeIdServerInterceptor())
            .build()
            .start()

        logger.info("gRPC Endpoint started on port ${config.grpcPort}.")
    }

    fun stop() {
        server.shutdown().awaitTermination()
    }
}