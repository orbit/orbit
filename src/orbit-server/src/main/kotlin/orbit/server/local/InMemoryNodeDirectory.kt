/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.common.util.RNGUtils
import orbit.server.net.LeaseExpiration
import orbit.server.net.NodeId
import orbit.server.net.NodeLease
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import java.time.Duration
import java.time.Instant

internal class InMemoryNodeDirectory(private val expiration: LeaseExpiration) : NodeDirectory {
    object InMemoryNodeDirectoryConfig : NodeDirectory.NodeDirectoryConfig {
        override val directoryType: Class<out NodeDirectory> = InMemoryNodeDirectory::class.java
        override val specificConfig: Any? = null
    }

    companion object Singleton {
        @JvmStatic
        var nodes: HashMap<NodeId, NodeInfo> = hashMapOf()
    }

    override suspend fun getNode(nodeId: NodeId): NodeInfo? {
        return nodes[nodeId]
    }

    override suspend fun lookupConnectedNodes(nodeId: NodeId): List<NodeInfo> {
        return nodes[nodeId]?.visibleNodes?.map { node -> nodes[node] }?.filterNotNull() ?: emptyList()
    }

    override suspend fun lookupMeshNodes(): List<NodeInfo.ServerNodeInfo> {
        return nodes.values.filterIsInstance<NodeInfo.ServerNodeInfo>()
    }

    override suspend fun report(node: NodeInfo) {
        if (node.id == NodeId.Empty) {
            println("node id empty")
            return
        }

        nodes[node.id] = node
    }

    override suspend fun <TNodeInfo : NodeInfo> join(nodeInfo: TNodeInfo): TNodeInfo {
        // TODO (brett) - Remove prefix as a diagnostic
        val nodeId = when (nodeInfo) {
            is NodeInfo.ServerNodeInfo -> NodeId.generate("mesh:")
            else -> NodeId.generate()
        }


        val lease = NodeLease(
            nodeId,
            expiresAt = Instant.now().plus(
                when (nodeInfo) {
                    is NodeInfo.ServerNodeInfo -> Duration.ofSeconds(100000)
                    else -> expiration.duration
                }
            ),

            renewAt = Instant.now().plus(expiration.renew),
            challengeToken = RNGUtils.secureRandomString()
        )

        val newNode = when (nodeInfo) {
            is NodeInfo.ServerNodeInfo -> nodeInfo.copy(nodeId, lease = lease)
            is NodeInfo.ClientNodeInfo -> nodeInfo.copy(nodeId, lease = lease)
            else -> nodeInfo
        }

        nodes[nodeId] = newNode

        return newNode as TNodeInfo
    }

    override suspend fun cullLeases() {
        val now = Instant.now()
        val leaseCount = nodes.count()

        val (expiredLeases, validLeases) = nodes.asIterable().partition { (id, node) -> node.lease.expiresAt < now }

        expiredLeases.forEach { (id) -> nodes.remove(id) }
        if (nodes.count() != leaseCount) {
            // TODO (brett) - remove this diagnostic message
            println("Leases culled from $leaseCount to ${nodes.count()}")
        }
    }
}