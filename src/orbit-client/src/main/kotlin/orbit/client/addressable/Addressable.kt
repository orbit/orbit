/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import orbit.client.OrbitClient
import orbit.shared.addressable.AddressableReference


/**
 * Denotes an addressable that does not have a concrete implementation.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NonConcrete

/**
 * Marker interface that determines an interface is addressable remotely.
 */
@NonConcrete
interface Addressable

/**
 * A class type which extends [Addressable].
 */
typealias AddressableClass = Class<out Addressable>

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
 * An abstract [Addressable] which allows Orbit to provide an [AddressableContext].
 */
abstract class AbstractAddressable {
    /**
     * The Orbit context. It will be available after the [Addressable] is registered with Orbit.
     * Attempting to access this variable before registration is undefined behavior.
     */
    lateinit var context: AddressableContext
}

/**
 * A context available to an [Addressable] which gives access to Orbit runtime information.
 */
data class AddressableContext(
    /**
     * A reference to this [Addressable].
     */
    val reference: AddressableReference,
    /**
     * A reference to the [OrbitClient].
     */
    val client: OrbitClient
)