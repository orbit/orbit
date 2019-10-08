/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import orbit.client.OrbitClientConfig
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeInfo
import java.util.concurrent.atomic.AtomicReference

class NodeStatus(config: OrbitClientConfig) {
    val serviceLocator = config.serviceLocator
    val latestInfo = AtomicReference<NodeInfo>()
    val capabilities = NodeCapabilities()
}