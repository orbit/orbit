/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.di

import java.util.concurrent.ConcurrentHashMap

class ComponentContainer {
    data class Registration<T>(
        val type: Class<T>,
        val factory: (ComponentContainer) -> T
    )

    private val registry = ConcurrentHashMap<Class<*>, Registration<*>>()

    init {
        register(ComponentContainer::class.java) { this }
    }

    fun <T : Any> register(type: Class<T>, factory: (ComponentContainer) -> T) {
        val registration = Registration(
            type = type,
            factory = factory
        )
        registry.remove(type)
        registry[type] = registration
    }

    fun <T> resolve(type: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        val registration = registry[type]
        return registration?.factory?.invoke(this) as T ?: construct(type)
    }


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

    inline fun <reified R : Any> resolve(): R {
        return resolve(R::class.java)
    }

    inline fun <reified R : Any> inject(): Lazy<R> = lazy {
        resolve<R>()
    }

    inline fun configure(config: ComponentContainerRoot.() -> Unit) {
        config(ComponentContainerRoot(this))
    }
}

class ComponentContainerRoot constructor(val container: ComponentContainer) {

    inline fun <reified T : Any> register(crossinline body: (ComponentContainer) -> T) =
        container.register(T::class.java) { c -> body(c) }

    inline fun <reified T : Any> register(crossinline body: () -> T) =
        container.register(T::class.java) { _ -> body() }

    inline fun <reified T : Any> singleton(clazz: Class<out T>) =
        lazy { container.construct(clazz) }.let {
            container.register(T::class.java) { _ -> it.value }
        }

    inline fun <reified T : Any> singleton() = singleton(T::class.java)

    inline fun <reified T : Any> instance(instance: T) =
        container.register(T::class.java) { _ -> instance }

    inline fun <reified T : Any> externallyConfigured(config: ExternallyConfigured<T>) {
        singleton(config.instanceType)
        container.register(config.javaClass) { config }
    }

    inline fun <reified T : Any> resolve(): T = container.resolve(T::class.java)
}

interface ExternallyConfigured<T> {
    val instanceType: Class<out T>
}