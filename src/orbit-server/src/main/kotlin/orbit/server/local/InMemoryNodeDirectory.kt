/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.common.util.RNGUtils
import orbit.server.net.NodeId
import orbit.server.net.NodeLease
import orbit.server.net.NodeLeases
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal class InMemoryNodeDirectory(private val expiration: NodeLeases.LeaseExpiration) : NodeDirectory {
    companion object Singleton {
        @JvmStatic
        var nodes: HashMap<NodeId, NodeInfo> = hashMapOf()
    }

    override suspend fun getNode(nodeId: NodeId): NodeInfo? {
        return nodes[nodeId]
    }

    override fun lookupConnectedNodes(nodeId: NodeId): Sequence<NodeInfo> {
        println("lookup nodes: $nodeId -> ${nodes[nodeId]!!.visibleNodes.map { node -> node.value }}")
        return nodes[nodeId]?.visibleNodes?.map { node -> nodes[node] }?.filterNotNull()?.asSequence()
            ?: emptySequence()
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
            expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plus(
                when (nodeInfo) {
                    is NodeInfo.ServerNodeInfo -> Duration.ofSeconds(100000)
                    else -> expiration.duration
                }
            ),
            renewAt = ZonedDateTime.now(ZoneOffset.UTC).plus(expiration.renew),
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

    override suspend fun removeNode(nodeId: NodeId) {
        nodes.remove(nodeId)
    }

    override suspend fun cullLeases() {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val leaseCount = nodes.count()

        val (expiredLeases, validLeases) = nodes.asIterable().partition { (id, node) -> node.lease.expiresAt < now }

        expiredLeases.forEach { (id) -> nodes.remove(id) }
        if (nodes.count() != leaseCount) {
            // TODO (brett) - remove this diagnostic message
            println("Leases culled from $leaseCount to ${nodes.count()}")
        }
    }
}
