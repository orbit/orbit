/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh.local

import orbit.server.mesh.NodeDirectory
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.util.concurrent.HashMapBackedAsyncMap
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock

class LocalNodeDirectory(private val clock: Clock) : HashMapBackedAsyncMap<NodeId, NodeInfo>(), NodeDirectory {
    object LocalNodeDirectorySingleton : ExternallyConfigured<NodeDirectory> {
        override val instanceType = LocalNodeDirectory::class.java
    }

    override suspend fun tick() {
        // Cull expired
        values().filter { clock.inPast(it.lease.expiresAt) }.also { toDelete ->
            toDelete.forEach {
                remove(it.id)
            }
        }
    }
}