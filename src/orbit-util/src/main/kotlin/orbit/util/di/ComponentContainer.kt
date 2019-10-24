/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.di

import java.util.concurrent.ConcurrentHashMap

class ComponentContainer {
    data class BeanDefinition<T>(
        val interfaceClass: Class<T>,
        val concreteClass: Class<out T>
    )

    private val beanDefinitions = ConcurrentHashMap<Class<*>, BeanDefinition<*>>()
    private val beanInstances = ConcurrentHashMap<Class<*>, Any>()

    init {
        registerInstance(this)
    }

    fun <T : Any> registerDefinition(concreteClass: Class<T>) = registerDefinition(concreteClass, concreteClass)
    fun <T : Any> registerDefinition(interfaceClass: Class<T>, concreteClass: Class<out T>) {
        val beanDef = BeanDefinition(
            interfaceClass = interfaceClass,
            concreteClass = concreteClass
        )
        beanDefinitions[interfaceClass] = beanDef
    }


    fun <T : Any> registerInstance(liveObject: T) = registerInstance(liveObject.javaClass, liveObject)
    fun <T : Any> registerInstance(interfaceClass: Class<T>, liveObject: T) {
        registerDefinition(interfaceClass, liveObject.javaClass)
        beanInstances[interfaceClass] = liveObject
    }

    fun <T> resolve(interfaceClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return beanInstances.getOrPut(interfaceClass) {
            val beanDef = beanDefinitions[interfaceClass]
                ?: error("No bean definition registered for '${interfaceClass.name}'.")
            construct(beanDef)
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> findInstances(interfaceClass: Class<T>): Collection<T> =
        beanInstances.filter { interfaceClass.isAssignableFrom(it.value.javaClass) }.map { it as T }

    inline fun <reified T> findInstances() = findInstances(T::class.java)

    fun <T> construct(beanDefinition: BeanDefinition<T>): T =
        construct(beanDefinition.concreteClass)

    fun <T> construct(concreteClass: Class<out T>): T {
        check(concreteClass.constructors.size == 1) { "${concreteClass.name} must have one constructor." }
        val ctr = concreteClass.constructors[0]
        val args = Array<Any?>(ctr.parameterCount) { null }
        ctr.parameters.forEachIndexed { i, arg ->
            args[i] = resolve(arg.type)
        }
        @Suppress("UNCHECKED_CAST")
        return ctr.newInstance(*args) as T
    }

    inline fun <reified T> construct() = construct(T::class.java)

    inline fun <reified R : Any> inject(): Lazy<R> = lazy {
        resolve(R::class.java)
    }

    inline fun configure(config: ComponentContainerRoot.() -> Unit) {
        config(ComponentContainerRoot(this))
    }

    fun debugString(): String {
        val defString = beanDefinitions
            .map { "${it.value.interfaceClass.name} -> ${it.value.concreteClass.name}" }
            .joinToString(
                prefix = "beanDefinitions[",
                separator = ", ",
                postfix = "]"
            )

        val instanceString = beanInstances
            .map {
                "${it.key.name} -> ${it.value.javaClass.name}@${Integer.toHexString(System.identityHashCode(it.value))}"
            }.joinToString(
                prefix = "beanInstances[",
                separator = ", ",
                postfix = "]"
            )

        return "$defString $instanceString"
    }
}

class ComponentContainerRoot constructor(val componentContainer: ComponentContainer) {
    inline fun <reified T : Any> definition(clazz: Class<out T>) =
        componentContainer.registerDefinition(T::class.java, clazz)

    inline fun <reified T : Any> definition(body: () -> Class<out T>) = definition(body())

    inline fun <reified T : Any> definition() =
        componentContainer.registerDefinition(T::class.java)

    inline fun <reified T : Any> instance(obj: T) =
        componentContainer.registerInstance(T::class.java, obj)

    inline fun <reified T : Any> instance(body: () -> T) = instance(body())

    inline fun <reified T : Any> resolve(): T = componentContainer.resolve(T::class.java)

    inline fun <reified T : Any> externallyConfigured(config: ExternallyConfigured<T>) {
        componentContainer.registerDefinition(T::class.java, config.instanceType)
        componentContainer.registerInstance(config.javaClass, config)
    }
}

interface ExternallyConfigured<T> {
    val instanceType: Class<out T>
}