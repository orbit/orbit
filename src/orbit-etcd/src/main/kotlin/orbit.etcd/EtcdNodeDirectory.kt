/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.etcd

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.options.GetOption
import kotlinx.coroutines.future.await
import orbit.server.mesh.NodeDirectory
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.proto.Node
import orbit.shared.proto.toNodeIdProto
import orbit.shared.proto.toNodeInfo
import orbit.shared.proto.toNodeInfoProto
import orbit.util.di.jvm.ExternallyConfigured
import java.nio.charset.Charset

class EtcdNodeDirectory(config: EtcdNodeDirectoryConfig) : NodeDirectory {
    data class EtcdNodeDirectoryConfig(
        val url: String
    ) : ExternallyConfigured<NodeDirectory> {
        override val instanceType: Class<out NodeDirectory> = EtcdNodeDirectory::class.java
    }

    init {
        println("Starting etcd node directory at ${config.url}")
    }

    private val client = Client.builder().endpoints(config.url).build().kvClient

    override suspend fun set(key: NodeId, value: NodeInfo) {
//        println("set val ${key}: ${value}")
        client.put(toKey(key), ByteSequence.from(key.toNodeIdProto().toByteArray())).await()
    }

    override suspend fun get(key: NodeId): NodeInfo? {
        val response = client.get(toKey(key)).await()
        val value = response.kvs.first().value

//        println("get val ${key}: ${value}")

        return Node.NodeInfoProto.parseFrom(value.bytes).toNodeInfo()
    }

    override suspend fun remove(key: NodeId): Boolean {
        client.delete(toKey(key))
        return true
    }

    override suspend fun compareAndSet(key: NodeId, initialValue: NodeInfo?, newValue: NodeInfo?): Boolean {
        val byteKey = toKey(key)
        val oldValue = client.get(byteKey).await().kvs.firstOrNull()?.value?.bytes?.let {
            Node.NodeInfoProto.parseFrom(it).toNodeInfo()
        }

//        println("compare and set ${key}: (i-${initialValue}, o-${oldValue}) -> ${newValue}")

        if (initialValue == oldValue) {
            if (newValue != null) {
                client.put(byteKey, ByteSequence.from(newValue.toNodeInfoProto().toByteArray())).await()
            } else {
                client.delete(byteKey).await()
            }
            return true
        }
        return false
    }

    override suspend fun entries(): Iterable<Pair<NodeId, NodeInfo>> {
        val key = ByteSequence.from("\u0000".toByteArray())

        val option = GetOption.newBuilder()
            .withSortField(GetOption.SortTarget.KEY)
            .withSortOrder(GetOption.SortOrder.DESCEND)
            .withRange(key)
            .build()

        val response = client.get(key, option).await()

        return response.kvs.map { kv ->
            Pair(
                fromKey(kv.key),
                Node.NodeInfoProto.parseFrom(kv.value.bytes).toNodeInfo()
            )
        }
    }

    fun toKey(nodeId: NodeId): ByteSequence {
        return ByteSequence.from("node/${nodeId.namespace}/${nodeId.key}".toByteArray())
    }

    fun fromKey(key: ByteSequence): NodeId {
        val keyString = key.toString(Charset.defaultCharset())

        val (_, namespace, key) = keyString.split("/")

        return NodeId(namespace, key)
    }

//    override suspend fun tick() {
//        val now = Instant.now()
//        val nodes = getAllNodes()
//        val leaseCount = nodes.count()
//
//        val (expiredLeases, validLeases) = nodes.partition { node -> node.lease.expiresAt < now }
//
//        if (expiredLeases.any()) {
//            val txn = client.txn()
//            txn.Then(*expiredLeases.map { node -> Op.delete(getKey(node.id), DeleteOption.DEFAULT) }.toTypedArray())
//                .commit()
//            println("Leases culled from $leaseCount to ${leaseCount - expiredLeases.count()}")
//        }
//    }
}
