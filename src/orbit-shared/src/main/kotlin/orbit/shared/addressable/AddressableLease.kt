/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.addressable

import orbit.shared.mesh.NodeId
import orbit.util.time.Timestamp

data class AddressableLease(
    val nodeId: NodeId,
    val reference: AddressableReference,
    val expiresAt: Timestamp,
    val renewAt: Timestamp
)