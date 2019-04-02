/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.hosting.DeferredWrappers
import cloud.orbit.runtime.pipeline.PipelineSystem
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class AddressableInterfaceClientProxy(
    private val pipelineSystem: PipelineSystem,
    private val reference: AddressableReference,
    private val target: NetTarget?
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        val addressableInvocation = AddressableInvocation(
            reference = reference,
            method = method,
            args = args ?: arrayOf()
        )

        val completion = pipelineSystem.pushInvocation(addressableInvocation, target)

        return DeferredWrappers.wrapReturn(completion, method)
    }
}

internal class AddressableInterfaceClientProxyFactory(
    private val pipelineSystem: PipelineSystem,
    private val definitionDirectory: AddressableDefinitionDirectory
) {
    fun <T : Addressable> createProxy(interfaceClass: Class<T>, key: Key, target: NetTarget? = null): T {
        val interfaceDefinition = definitionDirectory.getOrCreateInterfaceDefinition(interfaceClass)

        val invocationHandler = AddressableInterfaceClientProxy(
            pipelineSystem = pipelineSystem,
            reference = AddressableReference(
                interfaceClass = interfaceDefinition.interfaceClass,
                key = key
            ),
            target = target
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