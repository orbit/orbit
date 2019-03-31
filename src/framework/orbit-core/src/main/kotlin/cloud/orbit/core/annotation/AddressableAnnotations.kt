/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.annotation

import cloud.orbit.core.hosting.ExecutionStrategy
import cloud.orbit.core.hosting.RoutingStrategy
import cloud.orbit.core.remoting.Addressable
import kotlin.reflect.KClass

/**
 * Denotes an addressable that does not have a concrete implementation.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NonConcrete

/**
 * Denotes a method which is executed on activation for lifecycle managed [Addressable]s.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OnActivate

/**
 * Denotes a method which is executed on deactivation for lifecycle managed [Addressable]s.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OnDeactivate

/**
 * Determines the routing behavior for an [Addressable].
 * Must be present on the [Addressable] interface.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Routing(
    /**
     * Specifies whether the message is routed at all or if the destination should have been set externally.
     */
    val isRouted: Boolean,
    /**
     * Determines whether routing should take place if a destination is already set.
     */
    val forceRouting: Boolean,
    /**
     * The strategy to use when routing a message.
     */
    val routingStrategy: KClass<out RoutingStrategy>,
    /**
     * Determines whether routing persistently places an entry in the addressable directory.
     */
    val persistentPlacement: Boolean
)

/**
 * Determines the lifecycle behavior for an [Addressable].
 * Must be present on the [Addressable] implementation or interface.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Lifecycle(
    /**
     * Determines whether Orbit will construct and activate the [Addressable] for you.
     */
    val autoActivate: Boolean,
    /**
     * Determiens whether Orbit will deactivate and destroy the [Addressable] for you.
     */
    val autoDeactivate: Boolean
)

/**
 * Determines the execution behavior for an [Addressable].
 * Must be present on the [Addressable] implementation or interface.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ExecutionModel(
    val executionStrategy: ExecutionStrategy
)