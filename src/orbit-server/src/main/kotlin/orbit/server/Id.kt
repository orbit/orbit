/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

data class Id<T>(private val value: T) {
    override fun toString(): String {
        return value.toString()
    }
}
