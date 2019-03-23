/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.common.logging.debug
import cloud.orbit.common.logging.logger
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableClass
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class RemoteInterfaceDefinitionDictionary {
    private val interfaceDefinitionMap = ConcurrentHashMap<AddressableClass, RemoteInterfaceDefinition>()
    private val logger by logger()

    fun getOrCreate(interfaceClass: AddressableClass): RemoteInterfaceDefinition =
        interfaceDefinitionMap.getOrPut(interfaceClass) {
            generateInterfaceDefinition(interfaceClass)
        }

    private fun generateInterfaceDefinition(interfaceClass: AddressableClass): RemoteInterfaceDefinition {
        if (!interfaceClass.isInterface) {
            throw IllegalArgumentException("${interfaceClass.name} is not an interface.")
        }
        if (interfaceClass.isAnnotationPresent(NonConcrete::class.java)) {
            throw IllegalArgumentException("${interfaceClass.name} is non-concrete and can not be directly addressed")
        }

        val routing = findAddressableAnnotation(interfaceClass, Routing::class.java)
            ?: throw IllegalArgumentException("No @Routing found in interface hierarchy for ${interfaceClass.name}")

        val methods = interfaceClass.methods
            .map { method ->
                generateMethodDefinition(interfaceClass, method)
            }

        val definition = RemoteInterfaceDefinition(
            interfaceClass = interfaceClass,
            methods = methods,
            routing = routing
        )

        logger.debug { "Created definition: $definition" }

        return definition
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Annotation> findAddressableAnnotation(
        addressableClass: AddressableClass,
        annotation: Class<T>
    ): T? {
        val result = addressableClass.getAnnotation(annotation)
        if (result != null) return result
        addressableClass.interfaces
            .filter { Addressable::class.java.isAssignableFrom(it) }
            .map { it as AddressableClass }
            .forEach {
                val res = findAddressableAnnotation(it, annotation)
                if (res != null) return res
            }
        return null
    }

    private fun generateMethodDefinition(interfaceClass: AddressableClass, method: Method): RemoteMethodDefinition {
        return RemoteMethodDefinition(
            method = method
        )
    }
}