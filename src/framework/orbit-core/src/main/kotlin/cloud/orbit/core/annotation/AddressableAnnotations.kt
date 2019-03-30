/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.annotation

import cloud.orbit.core.remoting.Addressable

/**
 * Denotes a method which is executed on activation for lifecycle managed [Addressable]s.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class OnActivate

/**
 * Denotes a method which is executed on deactivation for lifecycle managed [Addressable]s.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class OnDeactivate