/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
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
    val logger by logger()

    lateinit var addressableInterfaces: List<AddressableClass>
    lateinit var addressableClasses: List<AddressableClass>
    lateinit var concreteAddressablesByClass: Map<AddressableClass, AddressableClass>
    lateinit var concreteAddressablesByInterface: Map<AddressableClass, AddressableClass>


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

        concreteAddressablesByClass = addressableClasses.map {
            val mapped = resolveMapping(it)
            it to mapped
        }.toMap()

        val tmpMap = mutableMapOf<AddressableClass, AddressableClass>()
        concreteAddressablesByClass.forEach { (k, v) ->
            if(tmpMap.containsKey(v)) throw IllegalStateException("Multiple implementations of concrete interface " +
                    "${v.name} found.")
            tmpMap[v] = k
        }
        concreteAddressablesByInterface = tmpMap


        logger.debug { "Addressable Interfaces: $addressableInterfaces" }
        logger.debug { "Addressable Classes: $addressableClasses" }
        logger.debug { "Concrete Addressables: $concreteAddressablesByClass" }

        logger.info {
            "Node capabilities scan complete in ${elapsed}ms. " +
                    "${concreteAddressablesByClass.size} concrete addressable(s) found. " +
                    "${addressableInterfaces.size} addressable interface(s) found. " +
                    "${addressableClasses.size} addressable class(es) found. "
        }
    }

    fun generateNodeCapabilities(): NodeCapabilities {
        val addressables = concreteAddressablesByInterface.map { (key, _) -> key.name }
        return NodeCapabilities(
            addressables = addressables
        )
    }

    private fun resolveMapping(addressable: AddressableClass): AddressableClass {
        fun doMapping(crawl: Class<*>, list: MutableList<AddressableClass> = mutableListOf()): List<AddressableClass> {
            if(crawl.interfaces.isEmpty()) return list
            for (iface in crawl.interfaces) {
                if (Addressable::class.java.isAssignableFrom(iface)) {
                    if (!iface.isAnnotationPresent(NonConcrete::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        list.add(iface as AddressableClass)
                    }
                    if(iface.interfaces.isNotEmpty()) doMapping(iface, list)
                }
            }
            return list
        }

        val results = doMapping(addressable)

        if(results.isEmpty()) {
            throw IllegalStateException("Could not find mapping for ${addressable.name}")
        } else if(results.size > 1) {
            throw IllegalStateException("More than one concrete interface found for ${addressable.name}")
        }

        return results.first()
    }
}