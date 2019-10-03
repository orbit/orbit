/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.mesh

import orbit.util.time.Timestamp

typealias ChallengeToken = String

data class NodeLease(
    val challengeToken: ChallengeToken,
    val expiresAt: Timestamp,
    val renewAt: Timestamp
)
