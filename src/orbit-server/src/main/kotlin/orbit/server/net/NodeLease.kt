/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import java.time.Instant

data class NodeLease(
    val nodeId: NodeId,
    val challengeToken: ChallengeToken,
    val expiresAt: Instant,
    val renewAt: Instant
) {
    companion object {
        @JvmStatic
        val Empty = NodeLease(
            NodeId.Empty,
            "",
            Instant.MIN,
            Instant.MIN
        )
    }
}
