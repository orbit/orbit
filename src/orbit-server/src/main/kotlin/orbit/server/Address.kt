/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

inline class AddressId(val value: String)

data class Address(val id: AddressId, val capability: Capability = Capability.None)

