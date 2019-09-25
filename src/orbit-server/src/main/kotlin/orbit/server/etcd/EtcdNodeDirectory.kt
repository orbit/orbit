/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.etcd

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.op.Op
import io.etcd.jetcd.options.DeleteOption
import io.etcd.jetcd.options.GetOption
import kotlinx.coroutines.future.await
import orbit.common.util.RNGUtils
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

class EtcdNodeDirectory(private val expiration: LeaseExpiration) : NodeDirectory {
    data class EtcdCreds(val username: String, val password: String)

    class EtcdNodeDirectoryConfig(private val etcdCreds: EtcdCreds) : NodeDirectory.NodeDirectoryConfig {
        override val directoryType: Class<out NodeDirectory> = EtcdNodeDirectory::class.java
        override val specificConfig: Any? = etcdCreds
    }

    private val client = Client.builder().endpoints("http://localhost:2379").build().kvClient

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
                    is NodeInfo.ServerNodeInfo -> Duration.ofSeconds(600)
                    else -> expiration.duration
                }
            ),
            renewAt = Instant.now().plus(
                when (nodeInfo) {
                    is NodeInfo.ServerNodeInfo -> Duration.ofSeconds(300)
                    else -> expiration.renew
                }
            ),
            challengeToken = RNGUtils.secureRandomString()
        )

        val newNode = when (nodeInfo) {
            is NodeInfo.ServerNodeInfo -> nodeInfo.copy(nodeId, lease = lease)
            is NodeInfo.ClientNodeInfo -> nodeInfo.copy(nodeId, lease = lease)
            else -> nodeInfo
        }

        client.put(getKey(nodeId), ByteSequence.from(newNode.toProto().toByteArray())).await()
        return newNode as TNodeInfo
    }

    override suspend fun report(nodeInfo: NodeInfo) {
        client.put(getKey(nodeInfo.id), ByteSequence.from(nodeInfo.toProto().toByteArray())).await()
    }

    override suspend fun getNode(nodeId: NodeId): NodeInfo? {
        val response = client.get(getKey(nodeId)).await()
        val value = response.kvs

        println("get node ${value}")

        return NodeInfo.ClientNodeInfo()
    }

    override suspend fun lookupConnectedNodes(nodeId: NodeId): List<NodeInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
