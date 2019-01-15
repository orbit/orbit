/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key
import cloud.orbit.core.remoting.Addressable
import java.lang.reflect.Proxy

class RemoteInterfaceProxyFactory(
    private val remoteInterfaceDefinitionDictionary: RemoteInterfaceDefinitionDictionary
) {
    fun <T : Addressable> getReference(interfaceType: Class<T>, key: Key): T {
        val remoteInterfaceDefinition = remoteInterfaceDefinitionDictionary.getOrCreate(interfaceType)
        val invocationHandler = RemoteInterfaceProxy(
            remoteInterfaceDefinition = remoteInterfaceDefinition,
            key = key
        )
        val javaProxy = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(remoteInterfaceDefinition.interfaceClass),
            invocationHandler
        )
        @Suppress("UNCHECKED_CAST")
        return javaProxy as T
    }
}