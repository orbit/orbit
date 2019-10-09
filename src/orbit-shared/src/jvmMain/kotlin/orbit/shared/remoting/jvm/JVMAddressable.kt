/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.remoting.jvm

/**
 * Denotes an addressable that does not have a concrete implementation.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NonConcrete

@NonConcrete
/**
 * Marker interface that determines an interface is addressable remotely.
 */
interface Addressable

/**
 * A class type which extends [Addressable].
 */
typealias AddressableClass = Class<out Addressable>
