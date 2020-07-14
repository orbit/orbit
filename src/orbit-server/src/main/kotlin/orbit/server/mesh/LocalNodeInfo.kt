/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import mu.KotlinLogging
import orbit.server.service.HealthCheck
import orbit.shared.exception.InvalidNodeId
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeStatus
import orbit.util.time.Clock
import orbit.util.time.Timestamp
import java.util.concurrent.atomic.AtomicReference

const val MANAGEMENT_NAMESPACE = "management"

class LocalNodeInfo(
    private val clusterManager: ClusterManager,
    private val clock: Clock,
    private val serverInfo: LocalServerInfo
) : HealthCheck {
    override suspend fun isHealthy(): Boolean {
        return this.info.nodeStatus == NodeStatus.ACTIVE
    }

    private val logger = KotlinLogging.logger {}

    val info: NodeInfo
        get() = infoRef.get().also {
            checkNotNull(it) { "LocalNodeInfo not initialized. " }
        }

    private val infoRef = AtomicReference<NodeInfo>()

    suspend fun updateInfo(body: (NodeInfo) -> NodeInfo) {
        clusterManager.updateNode(info.id) {
            checkNotNull(it) { "LocalNodeInfo not present in directory. ${info.id}" }
            body(it)
        }.also {
            infoRef.set(it)
        }
    }

    suspend fun start() {
        join()
    }

    suspend fun join(nodeStatus: NodeStatus = NodeStatus.STARTING) {
        clusterManager.joinCluster(MANAGEMENT_NAMESPACE, NodeCapabilities(), this.serverInfo.url, nodeStatus)
            .also {
                logger.info("Joined cluster as (${it.id})")
                infoRef.set(it)
            }
    }

    suspend fun tick() {
        if (clock.inPast(info.lease.renewAt)) {
            try {
                clusterManager.renewLease(info.id, info.lease.challengeToken, info.capabilities).also {
                    infoRef.set(it)
                }
            } catch (e: InvalidNodeId) {
                logger.info("Failed to renew lease, rejoining cluster.")
                join(NodeStatus.ACTIVE)
            }
        }
    }
}