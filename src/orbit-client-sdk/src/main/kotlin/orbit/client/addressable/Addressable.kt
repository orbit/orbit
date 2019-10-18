/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

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