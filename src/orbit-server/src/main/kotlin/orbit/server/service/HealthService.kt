/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import grpc.health.v1.HealthImplBase
import grpc.health.v1.HealthOuterClass
import io.micrometer.core.instrument.MeterRegistry

class HealthService(private val checks: HealthCheckList, private val metrics: MeterRegistry) : HealthImplBase() {
    private val counter = metrics.counter("orbit", "health", "check")
    override suspend fun check(request: HealthOuterClass.HealthCheckRequest): HealthOuterClass.HealthCheckResponse {
        counter.increment()
        return HealthOuterClass.HealthCheckResponse.newBuilder()
            .setStatus(
                if (this.isHealthy()) {
                    HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING
                } else {
                    HealthOuterClass.HealthCheckResponse.ServingStatus.NOT_SERVING
                }
            ).build()
    }

    suspend fun isHealthy(): Boolean {
        return checks.getChecks().all { check -> check.isHealthy() }
    }
}