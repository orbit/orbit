/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh.local

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import mu.KotlinLogging
import orbit.util.di.ExternallyConfigured

class LocalMeterRegistry : SimpleMeterRegistry() {
    private val logger = KotlinLogging.logger {}
    object LocalMeterRegistrySingleton : ExternallyConfigured<MeterRegistry> {
        override val instanceType = LocalMeterRegistry::class.java
    }

    init {
        logger.info("Starting simple meter registry")
    }
}