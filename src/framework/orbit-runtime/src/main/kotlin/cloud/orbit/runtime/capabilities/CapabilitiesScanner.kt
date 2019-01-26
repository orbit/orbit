/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.capabilities

import cloud.orbit.common.logging.debug
import cloud.orbit.common.logging.info
import cloud.orbit.common.logging.logger
import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.stopwatch
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.net.NodeCapabilities
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableClass
import io.github.classgraph.ClassGraph

class CapabilitiesScanner(private val clock: Clock) {
    private val logger by logger()

    lateinit var addressableInterfaces: List<AddressableClass> private set
    lateinit var addressableClasses: List<AddressableClass> private set
    lateinit var interfaceLookup: Map<AddressableClass, AddressableClass> private set


    fun scan(vararg packagePaths: String) {
        logger.info("Scanning for node capabilities...")

        val (elapsed, _) = stopwatch(clock) {
            val classGraph = ClassGraph()
                .enableAllInfo()
                .whitelistPackages(*packagePaths)

            val scan = classGraph.scan()

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
        }

        interfaceLookup = mutableMapOf<AddressableClass, AddressableClass>().apply {
            addressableClasses.forEach { implClass ->
                val mapped = resolveMapping(implClass)
                if (mapped.isEmpty()) {
                    throw IllegalStateException("Could not find mapping for ${implClass.name}")
                }
                mapped.forEach { iface ->
                    if (this.containsKey(iface)) throw IllegalStateException(
                        "Multiple implementations of concrete " +
                                "interface ${iface.name} found."
                    )
                    this[iface] = implClass
                }
            }
        }

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

    fun generateNodeCapabilities(): NodeCapabilities {
        val addressables = interfaceLookup.map { (key, _) -> key.name }
        return NodeCapabilities(
            implementedAddressables = addressables
        )
    }

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