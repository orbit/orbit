/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

inline class AddressId(val value: String)

open class Address(val id: AddressId) {
    open fun capability(): Capability = Capability.None
}

