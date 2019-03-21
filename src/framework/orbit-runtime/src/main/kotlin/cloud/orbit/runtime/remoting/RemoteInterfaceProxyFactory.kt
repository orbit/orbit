/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.runtime.pipeline.PipelineSystem
import java.lang.reflect.Proxy

class RemoteInterfaceProxyFactory(
    private val pipelineSystem: PipelineSystem,
    private val interfaceDefinitionDictionary: RemoteInterfaceDefinitionDictionary
) {
    fun <T : Addressable> getReference(interfaceClass: Class<T>, key: Key): T {
        val interfaceDefinition = interfaceDefinitionDictionary.getOrCreate(interfaceClass)

        val invocationHandler = RemoteInterfaceProxy(
            pipelineSystem = pipelineSystem,
            interfaceDefinition = interfaceDefinition,
            key = key
        )
        val javaProxy = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(interfaceClass),
            invocationHandler
        )
        @Suppress("UNCHECKED_CAST")
        return javaProxy as T
    }
}