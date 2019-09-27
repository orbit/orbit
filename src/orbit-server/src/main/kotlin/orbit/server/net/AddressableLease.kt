/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.server.addressable.AddressableReference
import java.time.Instant

data class AddressableLease(
    val address: AddressableReference,
    val nodeId: NodeId,
    val expiresAt: Instant,
    val renewAt: Instant
) {
    companion object {
        val Empty: AddressableLease = AddressableLease(
            AddressableReference(type = "", id = ""),
            nodeId = NodeId.Empty,
            expiresAt = Instant.MIN,
            renewAt = Instant.MIN
        )
    }
}
