/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.common.logging.debug
import cloud.orbit.common.logging.logger
import cloud.orbit.core.remoting.AddressableClass
import java.util.concurrent.ConcurrentHashMap

class RemoteInterfaceDefinitionDictionary {
    private val interfaceDefinitionMap = ConcurrentHashMap<AddressableClass, RemoteInterfaceDefinition>()
    private val logger by logger()

    fun getOrCreate(interfaceClass: AddressableClass): RemoteInterfaceDefinition =
        interfaceDefinitionMap.getOrPut(interfaceClass) {
            generateDefinition(interfaceClass)
        }

    private fun generateDefinition(interfaceClass: AddressableClass): RemoteInterfaceDefinition {
        val methods = interfaceClass.methods
            .map {
                RemoteMethodDefinition(
                    method = it,
                    methodName = it.name
                )
            }.map { it.method to it }
            .toMap()

        val interfaceDefinition = RemoteInterfaceDefinition(
            interfaceClass = interfaceClass,
            interfaceName = interfaceClass.name,
            methodDefinitions = methods
        )

        logger.debug { "Created definition: $interfaceDefinition" }

        return interfaceDefinition
    }
}