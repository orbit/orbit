/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.prometheus

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KotlinLogging
import orbit.util.di.ExternallyConfigured
import src.main.kotlin.orbit.prometheus.PrometheusMeterEndpoint


class PrometheusMetrics(config: PrometheusMetricsConfig) : PrometheusMeterRegistry(PrometheusConfig.DEFAULT) {
    data class PrometheusMetricsConfig(
        val url: String = "/metrics",
        val port: Int = 8080
    ) : ExternallyConfigured<MeterRegistry> {
        override val instanceType: Class<out MeterRegistry> = PrometheusMetrics::class.java
    }

    private val logger = KotlinLogging.logger {}
    private val meterServer = PrometheusMeterEndpoint(this, config.url, config.port)
}