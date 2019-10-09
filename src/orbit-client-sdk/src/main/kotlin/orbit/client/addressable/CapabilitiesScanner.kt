/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.capabilities

import io.github.classgraph.ClassGraph
import mu.KotlinLogging
import orbit.client.OrbitClientConfig
import orbit.util.time.Clock
import orbit.util.time.stopwatch

class CapabilitiesScanner(
    private val clock: Clock,
    config: OrbitClientConfig
) {
    private val logger = KotlinLogging.logger {}

    private val packagePaths = config.packages.toTypedArray()

    lateinit var addressableInterfaces: List<AddressableClass> private set
    lateinit var addressableClasses: List<AddressableClass> private set
    lateinit var interfaceLookup: Map<AddressableClass, AddressableClass> private set

    fun scan() {
        logger.info("Scanning for node capabilities...")

        stopwatch(clock) {
            val classGraph = ClassGraph()
                .enableAllInfo()
                .whitelistPackages(*packagePaths)

            classGraph.scan().use { scan ->

            }
        }
    }
}