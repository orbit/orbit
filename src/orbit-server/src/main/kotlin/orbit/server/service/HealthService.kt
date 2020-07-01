/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import grpc.health.v1.HealthImplBase
import grpc.health.v1.HealthOuterClass
import io.micrometer.core.instrument.Metrics
import orbit.util.instrumentation.recordSuspended
import java.util.concurrent.atomic.AtomicInteger

class HealthService(private val checks: HealthCheckList) : HealthImplBase() {
    private val counter = Metrics.counter("orbit", "health", "check")
    private val healthyChecks = AtomicInteger()

    init {
        Metrics.gauge(Meters.Names.PassingHealthChecks, healthyChecks)
    }

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
        val checks = checks.getChecks()
        Metrics.timer(Meters.Names.HealthCheck).recordSuspended {
            healthyChecks.set(checks.count { check -> check.isHealthy() })
        }
        return healthyChecks.get() == checks.count()
    }
}
