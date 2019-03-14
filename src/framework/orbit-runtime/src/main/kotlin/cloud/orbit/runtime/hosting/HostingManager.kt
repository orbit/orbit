/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.net.NodeIdentity
import cloud.orbit.runtime.net.NetManager
import cloud.orbit.runtime.remoting.RemoteInvocationTarget

class HostingManager(private val netManager: NetManager) {
    suspend fun locateOrPlace(rit: RemoteInvocationTarget): NodeIdentity {
        return netManager.localNode.nodeIdentity
    }

    suspend fun isLocal(rit: RemoteInvocationTarget): Boolean {
        return true
    }
}