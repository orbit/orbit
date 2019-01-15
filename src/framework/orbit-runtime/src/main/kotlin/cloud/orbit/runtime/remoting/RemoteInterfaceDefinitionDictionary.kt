/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.remoting.AddressableClass
import java.util.concurrent.ConcurrentHashMap

class RemoteInterfaceDefinitionDictionary {
    private val interfaceDefinitionMap = ConcurrentHashMap<AddressableClass, RemoteInterfaceDefinition>()

    fun getOrCreate(interfaceClass: AddressableClass): RemoteInterfaceDefinition =
        interfaceDefinitionMap.getOrPut(interfaceClass) {
            generateDefinition(interfaceClass)
        }

    private fun generateDefinition(interfaceClass: AddressableClass): RemoteInterfaceDefinition {
        return RemoteInterfaceDefinition(
            interfaceClass = interfaceClass,
            interfaceName = interfaceClass.name
        )
    }
}