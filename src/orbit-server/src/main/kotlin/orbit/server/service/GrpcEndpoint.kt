/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import io.grpc.Server
import io.grpc.ServerBuilder
import mu.KotlinLogging
import orbit.server.OrbitServerConfig

class GrpcEndpoint(
    private val serverAuthInterceptor: ServerAuthInterceptor,
    private val nodeManagementService: NodeManagementService,
    private val addressableManagementService: AddressableManagementService,
    private val connectionService: ConnectionService,
    config: OrbitServerConfig
) {
    private val logger = KotlinLogging.logger { }
    private lateinit var server: Server

    private val serverPort = config.serverPort

    fun start() {
        logger.info("Starting gRPC Endpoint on $serverPort...")

        server = ServerBuilder.forPort(serverPort.port)
            .intercept(serverAuthInterceptor)
            .addService(nodeManagementService)
            .addService(addressableManagementService)
            .addService(connectionService)
            .build()
            .start()

        logger.info("gRPC Endpoint started on $serverPort.")
    }

    fun stop() {
        server.shutdownNow().awaitTermination()
    }
}