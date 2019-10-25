/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.server.OrbitServerConfig
import orbit.shared.exception.InvalidChallengeException
import orbit.shared.exception.InvalidNodeId
import orbit.shared.mesh.ChallengeToken
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeLease
import orbit.util.misc.RNGUtils
import orbit.util.time.Timestamp
import orbit.util.time.toTimestamp
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ClusterManager(
    config: OrbitServerConfig,
    private val nodeDirectory: NodeDirectory
) {
    private val leaseExpiration = config.nodeLeaseDuration

    private val clusterNodes = ConcurrentHashMap<NodeId, NodeInfo>()

    val allNodes get() = clusterNodes.values.toList()

    suspend fun tick() {
        clusterNodes.clear()
        nodeDirectory.values().forEach {
            clusterNodes[it.id] = it
        }
    }

    suspend fun joinCluster(namespace: String, capabilities: NodeCapabilities, url: String? = null): NodeInfo {
        do {
            val newNodeId = NodeId.generate(namespace)

            val lease = NodeLease(
                challengeToken = RNGUtils.randomString(64),
                expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
            )

            val info = NodeInfo(
                id = newNodeId,
                capabilities = capabilities,
                lease = lease,
                url = url
            )

            if (nodeDirectory.compareAndSet(newNodeId, null, info)) {
                return info
            }
        } while (true)
    }

    suspend fun renewLease(nodeId: NodeId, challengeToken: ChallengeToken, capabilities: NodeCapabilities): NodeInfo =
        updateNode(nodeId) { initialValue ->
            if (initialValue == null || Timestamp.now() > initialValue.lease.expiresAt) {
                throw InvalidNodeId(nodeId)
            }

            if (initialValue.lease.challengeToken != challengeToken) {
                throw InvalidChallengeException(nodeId, challengeToken)
            }

            initialValue.copy(
                capabilities = capabilities,
                lease = initialValue.lease.copy(
                    expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                    renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
                )
            )
        }!!

    suspend fun updateNode(nodeId: NodeId, body: (NodeInfo?) -> NodeInfo?): NodeInfo? =
        nodeDirectory.manipulate(nodeId, body)

    suspend fun getNode(nodeId: NodeId, forceRefresh: Boolean = false): NodeInfo? =
        if (forceRefresh) {
            nodeDirectory.get(nodeId)
        } else {
            try {
                clusterNodes.getOrPut(nodeId) {
                    nodeDirectory.get(nodeId)
                }
            } catch (t: NullPointerException) {
                null
            }
        }?.let {
            if (it.lease.expiresAt > Timestamp.now()) {
                it
            } else {
                null
            }
        }
}
