/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.mesh

typealias AddressableType = String
typealias AddressableId = String

data class AddressableReference(
    val type: AddressableType,
    val id: AddressableId
)