/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.server.service.HealthCheck
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.util.concurrent.AsyncMap

interface AddressableDirectory : AsyncMap<AddressableReference, AddressableLease>,
    HealthCheck {
    suspend fun tick() {}
    suspend fun count(): Int = 0
}