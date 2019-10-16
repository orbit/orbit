/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import io.github.classgraph.ClassGraph
import mu.KotlinLogging
import orbit.client.OrbitClientConfig
import orbit.shared.mesh.Addressable
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NonConcrete
import orbit.shared.mesh.jvm.AddressableClass
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
                addressableInterfaces = scan
                    .getClassesImplementing(Addressable::class.java.name)
                    .interfaces
                    .filter {
                        !it.hasAnnotation(NonConcrete::class.java.name)
                    }
                    .map {
                        @Suppress("UNCHECKED_CAST")
                        it.loadClass() as AddressableClass
                    }

                addressableClasses = scan
                    .getClassesImplementing(Addressable::class.java.name)
                    .standardClasses
                    .filter {
                        !it.hasAnnotation(NonConcrete::class.java.name)
                    }
                    .filter {
                        !it.isAbstract
                    }
                    .map {
                        @Suppress("UNCHECKED_CAST")
                        it.loadClass() as AddressableClass
                    }

                interfaceLookup = mutableMapOf<AddressableClass, AddressableClass>().apply {
                    addressableClasses.forEach { implClass ->
                        resolveMapping(implClass).also { mapped ->
                            check(!mapped.isEmpty()) { "Could not find mapping for ${implClass.name}" }

                            mapped.forEach { iface ->
                                check(!this.containsKey(iface)) { "Multiple implementations of concrete interface ${iface.name} found." }
                                this[iface] = implClass
                            }
                        }
                    }
                }
            }
        }.also { (elapsed, _) ->
            logger.debug { "Addressable Interfaces: $addressableInterfaces" }
            logger.debug { "Addressable Classes: $addressableClasses" }
            logger.debug { "Implemented Addressables: $interfaceLookup" }

            logger.info {
                "Node capabilities scan complete in ${elapsed}ms. " +
                        "${interfaceLookup.size} implemented addressable(s) found. " +
                        "${addressableInterfaces.size} addressable interface(s) found. " +
                        "${addressableClasses.size} addressable class(es) found. "
            }
        }
    }

    fun generateCapabilities() = NodeCapabilities(
        addressableTypes = interfaceLookup.map { (key, _) -> key.name }
    )

    private fun resolveMapping(
        crawl: Class<*>,
        list: MutableList<AddressableClass> = mutableListOf()
    ): Collection<AddressableClass> {
        if (crawl.interfaces.isEmpty()) return list
        for (iface in crawl.interfaces) {
            if (Addressable::class.java.isAssignableFrom(iface)) {
                if (!iface.isAnnotationPresent(NonConcrete::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    list.add(iface as AddressableClass)
                }
                if (iface.interfaces.isNotEmpty()) resolveMapping(iface, list)
            }
        }
        return list
    }
}