/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

/**
 * Contains information about a type.
 *
 * @param name the type name.
 * @param arity the number of type parameters this type takes.
 */
data class TypeDescriptor(val name: String, val arity: Int)
