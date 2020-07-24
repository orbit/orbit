/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import orbit.shared.mesh.ChallengeToken
import orbit.shared.mesh.NodeLease
import orbit.util.time.Timestamp

val NodeLease.Companion.expired: NodeLease
    get() = NodeLease(
        ChallengeToken(),
        Timestamp(0, 0),
        Timestamp(0, 0)
    )

val NodeLease.Companion.forever: NodeLease
    get() = NodeLease(
        ChallengeToken(),
        Timestamp(Long.MAX_VALUE, Int.MAX_VALUE),
        Timestamp(Long.MAX_VALUE, Int.MAX_VALUE)
    )
