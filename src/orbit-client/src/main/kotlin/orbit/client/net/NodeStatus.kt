/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import orbit.client.leasing.NodeLease
import java.util.concurrent.atomic.AtomicReference

class NodeStatus {
    val currentLease = AtomicReference<NodeLease>()

    val capabilities = emptyList<String>()
}