/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.core.net.ClusterInfo
import cloud.orbit.core.net.NodeInfo
import java.util.concurrent.atomic.AtomicReference

class NetManager {
    private val localNode = AtomicReference<NodeInfo>()
    private val clusterInfo = AtomicReference<ClusterInfo>()
}