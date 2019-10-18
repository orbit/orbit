/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeInfo
import orbit.util.time.Timestamp
import orbit.util.time.now
import java.util.concurrent.atomic.AtomicReference

const val MANAGEMENT_NAMESPACE = "management"

class LocalNodeInfo(
    private val clusterManager: ClusterManager
) {
    val info: NodeInfo
        get() = infoRef.get().also {
            checkNotNull(it) { "LocalNodeInfo not initialized. " }
        }

    private val infoRef = AtomicReference<NodeInfo>()

    suspend fun updateInfo(body: (NodeInfo) -> NodeInfo) {
        clusterManager.updateNode(info.id) {
            checkNotNull(it) { "LocalNodeInfo not present in directory. " }
            body(it)
        }.also {
            infoRef.set(it)
        }
    }

    suspend fun start() {
        clusterManager.joinCluster(MANAGEMENT_NAMESPACE, NodeCapabilities()).also {
            println("Connected local node ${it.id}")
            infoRef.set(it)
        }
    }

    suspend fun tick() {
        if (Timestamp.now() > info.lease.renewAt) {
            clusterManager.renewLease(info.id, info.lease.challengeToken, info.capabilities).also {
                infoRef.set(it)
            }
        }
    }
}