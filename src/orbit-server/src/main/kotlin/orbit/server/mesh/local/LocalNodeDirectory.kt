/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh.local

import orbit.util.concurrent.jvm.HashMapBackedAsyncMap
import orbit.util.di.jvm.ExternallyConfigured
import orbit.server.mesh.NodeDirectory
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.util.time.Timestamp
import orbit.util.time.now

class LocalNodeDirectory : HashMapBackedAsyncMap<NodeId, NodeInfo>(), NodeDirectory {
    object LocalNodeDirectoryConfig : ExternallyConfigured<NodeDirectory> {
        override val instanceType = LocalNodeDirectory::class.java
    }

    override suspend fun tick() {
        // Cull expired
        values().filter { it.lease.expiresAt < Timestamp.now() }.also { toDelete ->
            toDelete.forEach {
                remove(it.id)
            }
        }
    }
}