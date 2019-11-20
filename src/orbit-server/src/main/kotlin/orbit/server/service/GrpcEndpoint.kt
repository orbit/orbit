/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import mu.KotlinLogging
import orbit.server.mesh.LocalServerInfo
import java.util.concurrent.TimeUnit

class GrpcEndpoint(
    private val serverAuthInterceptor: ServerAuthInterceptor,
    private val healthService: HealthService,
    private val nodeManagementService: NodeManagementService,
    private val addressableManagementService: AddressableManagementService,
    private val connectionService: ConnectionService,
    private val localServerInfo: LocalServerInfo
) {
    private val logger = KotlinLogging.logger { }
    private lateinit var server: Server

    fun start() {
        logger.info("Starting gRPC Endpoint on $localServerInfo.port...")

        server = NettyServerBuilder.forPort(localServerInfo.port)
            .keepAliveTime(500, TimeUnit.MILLISECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .permitKeepAliveWithoutCalls(true)
            .permitKeepAliveTime(1000, TimeUnit.MILLISECONDS)
            .intercept(serverAuthInterceptor)
            .addService(healthService)
            .addService(nodeManagementService)
            .addService(addressableManagementService)
            .addService(connectionService)
            .build()
            .start()

        logger.info("gRPC Endpoint started on $localServerInfo.port.")
    }

    fun stop() {
        server.shutdownNow().awaitTermination()
    }
}