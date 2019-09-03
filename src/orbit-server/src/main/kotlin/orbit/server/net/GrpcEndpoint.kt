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
import orbit.server.demo.GreeterImpl
import orbit.server.pipeline.Pipeline
import orbit.server.routing.NodeDirectory
import orbit.server.routing.Router

internal class GrpcEndpoint(private val config: OrbitServerConfig, private val pipeline: Pipeline, private val nodeDirectory: NodeDirectory) {
    private lateinit var server: Server

    private val logger by logger()

    fun start() {
        logger.info("Starting gRPC Endpoint on port ${config.grpcPort}...")

        server = ServerBuilder.forPort(config.grpcPort)
            .addService(GreeterImpl())
            .addService(ClientConnections(pipeline, config.nodeId, nodeDirectory))
            .intercept(ConnectionInterceptor())
            .build()
            .start()

        logger.info("gRPC Endpoint started on port ${config.grpcPort}.")
    }

    fun stop() {
        server.shutdown().awaitTermination()
    }
}