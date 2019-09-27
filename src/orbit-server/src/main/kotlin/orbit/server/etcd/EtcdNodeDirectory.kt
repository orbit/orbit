/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.etcd

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.Lease
import io.etcd.jetcd.op.Op
import io.etcd.jetcd.options.DeleteOption
import io.etcd.jetcd.options.GetOption
import kotlinx.coroutines.future.await
import orbit.common.util.RNGUtils
import orbit.server.config.InjectedWithConfig
import orbit.server.net.LeaseExpiration
import orbit.server.net.NodeId
import orbit.server.net.NodeLease
import orbit.server.proto.fromProto
import orbit.server.proto.toProto
import orbit.server.routing.NodeDirectory
import orbit.server.routing.NodeInfo
import orbit.shared.proto.NodeManagementOuterClass
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant

class EtcdNodeDirectory(private val config: EtcdNodeDirectoryConfig) : NodeDirectory {
    data class EtcdNodeDirectoryConfig(
        val url: String,
        val clientExpiration: LeaseExpiration,
        val serverExpiration: LeaseExpiration
    ) :
        InjectedWithConfig<NodeDirectory> {
        override val instanceType: Class<out NodeDirectory> = EtcdNodeDirectory::class.java
    }

    init {
        println("Starting etcd node directory at ${config.url}")
    }

    private val client = Client.builder().endpoints(config.url).build().kvClient

    fun getKey(nodeId: NodeId): ByteSequence {
        return ByteSequence.from(nodeId.value.toByteArray())
    }

    override suspend fun <TNodeInfo : NodeInfo> join(nodeInfo: TNodeInfo): TNodeInfo {
        val nodeId = when (nodeInfo) {
            is NodeInfo.ServerNodeInfo -> NodeId.generate("mesh:")
            else -> NodeId.generate()
        }

        val lease = NodeLease(
            nodeId,
            expiresAt = Instant.now().plus(
                when (nodeInfo) {
                    is NodeInfo.ServerNodeInfo -> config.serverExpiration.duration
                    else -> config.clientExpiration.duration
                }
            ),
            renewAt = Instant.now().plus(
                when (nodeInfo) {
                    is NodeInfo.ServerNodeInfo -> config.serverExpiration.duration
                    else -> config.clientExpiration.renew
                }
            ),
            challengeToken = RNGUtils.secureRandomString()
        )

        val newNode = nodeInfo.clone(nodeId, lease = lease)

        client.put(getKey(nodeId), ByteSequence.from(newNode.toProto().toByteArray())).await()
        return newNode as TNodeInfo
    }

    override suspend fun report(nodeInfo: NodeInfo) {
        println("Reporting node ${nodeInfo.id}. Expires ${nodeInfo.lease.expiresAt} VisibleNodes: ${nodeInfo.visibleNodes}")
        client.put(getKey(nodeInfo.id), ByteSequence.from(nodeInfo.toProto().toByteArray())).await()
    }

    override suspend fun getNode(nodeId: NodeId): NodeInfo? {
        val response = client.get(getKey(nodeId)).await()
        val value = response.kvs.first().value
        println("get node ${nodeId}")

        return NodeInfo.fromProto(NodeManagementOuterClass.NodeInfo.parseFrom(value.bytes))
    }

    override suspend fun lookupConnectedNodes(nodeId: NodeId): List<NodeInfo> {
        val node1 = getNode(nodeId)
        val visibleNodes = node1?.visibleNodes ?: return listOf()

        val allNodes = getAllNodes()

        return allNodes.filter { node -> visibleNodes.contains(node.id) }
    }

    override suspend fun lookupMeshNodes(): List<NodeInfo.ServerNodeInfo> {
        val nodes = getAllNodes()

        return nodes.filterIsInstance<NodeInfo.ServerNodeInfo>()
    }

    suspend fun getAllNodes(): List<NodeInfo> {
        val key = ByteSequence.from("\u0000".toByteArray())

        val option = GetOption.newBuilder()
            .withSortField(GetOption.SortTarget.KEY)
            .withSortOrder(GetOption.SortOrder.DESCEND)
            .withRange(key)
            .build()

        val response = client.get(key, option).await()

        val nodes = response.kvs.map { kv ->
            Pair(
                NodeId(kv.key.toString(Charset.defaultCharset())),
                NodeInfo.fromProto(NodeManagementOuterClass.NodeInfo.parseFrom(kv.value.bytes))
            )
        }.toMap()

        return nodes.values.toList()
    }

    override suspend fun cullLeases() {
        val now = Instant.now()
        val nodes = getAllNodes()
        val leaseCount = nodes.count()

        val (expiredLeases, validLeases) = nodes.partition { node -> node.lease.expiresAt < now }

        if (expiredLeases.any()) {
            val txn = client.txn()
            txn.Then(*expiredLeases.map { node -> Op.delete(getKey(node.id), DeleteOption.DEFAULT) }.toTypedArray())
                .commit()
            println("Leases culled from $leaseCount to ${leaseCount - expiredLeases.count()}")
        }
    }
}
