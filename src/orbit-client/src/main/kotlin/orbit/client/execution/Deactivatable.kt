/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import orbit.client.addressable.DeactivationReason
import orbit.client.net.Completion
import orbit.shared.addressable.AddressableReference

interface Deactivatable {
    fun deactivate(deactivationReason: DeactivationReason): Completion
    val reference: AddressableReference
}