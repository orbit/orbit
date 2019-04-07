/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.common.logging.debug
import cloud.orbit.common.logging.logger
import cloud.orbit.common.util.AnnotationUtils
import cloud.orbit.core.annotation.ExecutionModel
import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.annotation.OnActivate
import cloud.orbit.core.annotation.OnDeactivate
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.remoting.AddressableClass
import cloud.orbit.runtime.hosting.DeferredWrappers
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

internal class AddressableDefinitionDirectory {
    private val interfaceDefinitionMap = ConcurrentHashMap<AddressableClass, AddressableInterfaceDefinition>()
    private val implDefinitionMap = ConcurrentHashMap<AddressableClass, AddressableImplDefinition>()
    private val interfaceImplComboImplDefinitionMap =
        ConcurrentHashMap<Pair<AddressableClass, AddressableClass>, AddressableImplDefinition>()

    private val logger by logger()

    fun setupDefinition(interfaceClasses: Iterable<AddressableClass>, impls: Map<AddressableClass, AddressableClass>) {
        interfaceClasses.forEach {
            getOrCreateInterfaceDefinition(it)
        }

        impls.forEach { (interfaceClass, implClass) ->
            val implDef = generateImplDefinition(interfaceClass, implClass)
            implDefinitionMap[interfaceClass] = implDef
        }
    }

    fun getOrCreateInterfaceDefinition(interfaceClass: AddressableClass): AddressableInterfaceDefinition =
        interfaceDefinitionMap.getOrPut(interfaceClass) {
            generateInterfaceDefinition(interfaceClass)
        }

    fun getImplDefinition(interfaceClass: AddressableClass): AddressableImplDefinition =
        implDefinitionMap[interfaceClass] ?: throw IllegalStateException("No implementation found for $interfaceClass")

    fun onDemandImplClass(interfaceClass: AddressableClass, implClass: AddressableClass): AddressableImplDefinition =
        interfaceImplComboImplDefinitionMap.getOrPut(interfaceClass to implClass) {
            generateImplDefinition(interfaceClass, implClass)
        }

    private fun generateInterfaceDefinition(interfaceClass: AddressableClass): AddressableInterfaceDefinition {
        if (!interfaceClass.isInterface) {
            throw IllegalArgumentException("${interfaceClass.name} is not an interface.")
        }
        if (interfaceClass.isAnnotationPresent(NonConcrete::class.java)) {
            throw IllegalArgumentException("${interfaceClass.name} is non-concrete and can not be directly addressed")
        }

        val routing = AnnotationUtils.findAnnotation(interfaceClass, Routing::class.java)
            ?: throw IllegalArgumentException(
                "No routing annotation found in hierarchy for " +
                        interfaceClass.name
            )

        val methods = interfaceClass.methods
            .map { method ->
                method to generateInterfaceMethodDefinition(method)
            }.toMap()

        val definition = AddressableInterfaceDefinition(
            interfaceClass = interfaceClass,
            methods = methods,
            routing = routing
        )

        logger.debug { "Created interface definition: $definition" }

        return definition
    }

    private fun generateInterfaceMethodDefinition(
        method: Method
    ): AddressableInterfaceMethodDefinition {

        verifyMethodIsAsync(method)

        return AddressableInterfaceMethodDefinition(
            method = method
        )
    }

    private fun generateImplDefinition(
        interfaceClass: AddressableClass,
        implClass: AddressableClass
    ): AddressableImplDefinition {
        val interfaceDef = getOrCreateInterfaceDefinition(interfaceClass)

        val lifecycle = AnnotationUtils.findAnnotation(implClass, Lifecycle::class.java)
            ?: throw IllegalArgumentException(
                "No lifecycle annotation found in hierarchy for " +
                        interfaceClass.name
            )

        val executionModel = AnnotationUtils.findAnnotation(implClass, ExecutionModel::class.java)
            ?: throw IllegalArgumentException(
                "No execution model annotation found in hierarchy for " +
                        interfaceClass.name
            )

        val methods = implClass.methods
            .map { method ->
                method to generateImplMethodDefinition(method)
            }.toMap()

        val onActivateMethod = methods.values.singleOrNull { it.isOnActivate }
        val onDeactivateMethod = methods.values.singleOrNull { it.isOnDeactivate }

        val definition = AddressableImplDefinition(
            interfaceClass = interfaceClass,
            implClass = implClass,
            interfaceDefinition = interfaceDef,
            methods = methods,
            onActivateMethod = onActivateMethod,
            onDeactivateMethod = onDeactivateMethod,
            lifecycle = lifecycle,
            executionModel = executionModel
        )

        logger.debug { "Created implementation definition: $definition" }

        return definition
    }

    private fun generateImplMethodDefinition(
        method: Method
    ): AddressableImplMethodDefinition {

        val isOnActivate = method.isAnnotationPresent(OnActivate::class.java)
        val isOnDeactivate = method.isAnnotationPresent(OnDeactivate::class.java)

        if (isOnActivate || isOnDeactivate) {
            verifyMethodIsAsync(method)
        }

        return AddressableImplMethodDefinition(
            method = method,
            isOnActivate = isOnActivate,
            isOnDeactivate = isOnDeactivate
        )
    }

    private fun verifyMethodIsAsync(method: Method) {
        if (!DeferredWrappers.canHandle(method.returnType))
            throw IllegalArgumentException("Method $method does not return asynchronous type.")
    }
}