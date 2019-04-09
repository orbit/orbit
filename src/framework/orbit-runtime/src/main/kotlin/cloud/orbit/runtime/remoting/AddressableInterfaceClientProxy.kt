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
    val reference: AddressableReference,
    val interfaceDefinition: AddressableInterfaceDefinition,
    val target: NetTarget?
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any =
        interfaceDefinition.getMethod(method).invocationType
            .let {
                AddressableInvocation(
                    reference = reference,
                    invocationType = it,
                    method = method,
                    args = args ?: arrayOf()
                )
            }.let {
                pipelineSystem.pushInvocation(it, target)
            }.let {
                return DeferredWrappers.wrapReturn(it, method)
            }
}

internal class AddressableInterfaceClientProxyFactory(
    private val pipelineSystem: PipelineSystem,
    private val definitionDirectory: AddressableDefinitionDirectory
) {
    fun <T : Addressable> createProxy(interfaceClass: Class<T>, key: Key, target: NetTarget? = null): T =
        interfaceClass.getOrCreateInterfaceDefinition(definitionDirectory).let {
            AddressableInterfaceClientProxy(
                pipelineSystem = pipelineSystem,
                interfaceDefinition = it,
                reference = AddressableReference(
                    interfaceClass = it.interfaceClass,
                    key = key
                ),
                target = target
            )
        }.let {
            @Suppress("UNCHECKED_CAST")
            Proxy.newProxyInstance(
                javaClass.classLoader,
                arrayOf(interfaceClass),
                it
            ) as T
        }
}