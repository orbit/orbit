/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.di

import java.util.concurrent.ConcurrentHashMap

internal class ComponentProvider {
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

    @Suppress("UNCHECKED_CAST")
    fun <T> resolve(interfaceClass: Class<T>): T {
        return beanInstances.getOrPut(interfaceClass) {
            val beanDef = beanDefinitions.get(interfaceClass)
                ?: throw IllegalStateException("No bean definition registered for '${interfaceClass.name}'.")
            if (beanDef.concreteClass.constructors.size != 1)
                throw IllegalStateException("${beanDef.concreteClass.name} must have one constructor.")
            val ctr = beanDef.concreteClass.constructors[0]
            val args = Array<Any?>(ctr.parameterCount) { null }
            ctr.parameters.forEachIndexed { i, arg ->
                args[i] = resolve(arg.type)
            }
            ctr.newInstance(*args) as T
        } as T
    }

    inline fun <reified R : Any> inject(): Lazy<R> = lazy {
        resolve(R::class.java)
    }

    inline fun configure(config: ComponentProviderRoot.() -> Unit) {
        config(ComponentProviderRoot(this))
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

internal class ComponentProviderRoot constructor(private val componentProvider: ComponentProvider) {
    inline fun <reified T : Any> definition(body: () -> Class<out T>) =
        componentProvider.registerDefinition(T::class.java, body())

    inline fun <reified T : Any> definition() =
        componentProvider.registerDefinition(T::class.java)

    inline fun <reified T : Any> instance(body: () -> T) =
        componentProvider.registerInstance(T::class.java, body())

    inline fun <reified T : Any> resolve(): T = componentProvider.resolve(T::class.java)
}