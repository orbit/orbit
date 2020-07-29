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
import mu.KotlinLogging
import orbit.server.mesh.NodeDirectory
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.proto.Node
import orbit.shared.proto.toNodeInfo
import orbit.shared.proto.toNodeInfoProto
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import orbit.util.time.stopwatch
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class EtcdNodeDirectory(config: EtcdNodeDirectoryConfig, private val clock: Clock) : NodeDirectory {
    data class EtcdNodeDirectoryConfig(
        val url: String,
        val cleanupFrequencyRange: Pair<Duration, Duration> = Duration.ofMinutes(1) to Duration.ofMinutes(2)
    ) : ExternallyConfigured<NodeDirectory> {
        override val instanceType: Class<out NodeDirectory> = EtcdNodeDirectory::class.java
    }

    private val keyPrefix = "node"
    private val allKey = ByteSequence.from("\u0000".toByteArray())

    private val logger = KotlinLogging.logger { }

    private val client = Client.builder().endpoints(config.url).build().kvClient
    private val lastCleanup = AtomicLong(clock.currentTime)
    private val cleanupIntervalMs =
        Random.nextLong(config.cleanupFrequencyRange.first.toMillis(), config.cleanupFrequencyRange.second.toMillis())

    override suspend fun get(key: NodeId): NodeInfo? {
        val response = client.get(toByteKey(key)).await()
        return response.kvs.firstOrNull()?.value?.let {
            Node.NodeInfoProto.parseFrom(it.bytes).toNodeInfo()
        }
    }

    override suspend fun remove(key: NodeId): Boolean {
        client.delete(toByteKey(key)).await()
        return true
    }

    override suspend fun compareAndSet(key: NodeId, initialValue: NodeInfo?, newValue: NodeInfo?): Boolean {
        val byteKey = toByteKey(key)
        val oldValue = client.get(byteKey).await().kvs.firstOrNull()?.value?.bytes?.let {
            Node.NodeInfoProto.parseFrom(it).toNodeInfo()
        }

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

    override suspend fun count() =
        client.get(
            allKey, GetOption.newBuilder()
                .withSortField(GetOption.SortTarget.KEY)
                .withSortOrder(GetOption.SortOrder.DESCEND)
                .withPrefix(ByteSequence.from(keyPrefix.toByteArray()))
                .withCountOnly(true)
                .withRange(allKey)
                .build()
        ).await().count

    override suspend fun entries(): Iterable<Pair<NodeId, NodeInfo>> {
        val option = GetOption.newBuilder()
            .withSortField(GetOption.SortTarget.KEY)
            .withSortOrder(GetOption.SortOrder.DESCEND)
            .withPrefix(ByteSequence.from(keyPrefix.toByteArray()))
            .withRange(allKey)
            .build()

        val response = client.get(allKey, option).await()

        return response.kvs.map { kv ->
            Pair(
                fromByteKey(kv.key),
                Node.NodeInfoProto.parseFrom(kv.value.bytes).toNodeInfo()
            )
        }
    }

    override suspend fun tick() {
        if (lastCleanup.get() + cleanupIntervalMs < clock.currentTime) {
            val (time, cleanupResult) = stopwatch(clock) {
                lastCleanup.set(clock.currentTime)

                val (expiredLeases, validLeases) =
                    entries().partition { (_, node) -> clock.inPast(node.lease.expiresAt) }

                if (expiredLeases.any()) {
                    val txn = client.txn()
                    txn.Then(*expiredLeases.map { (id) ->
                        Op.delete(toByteKey(id), DeleteOption.DEFAULT)
                    }.toTypedArray()).commit().await()
                }

                object {
                    val expired = expiredLeases.count()
                    val valid = validLeases.count()
                }
            }

            logger.info {
                "Node Directory cleanup took ${time}ms. Removed ${cleanupResult.expired} entries, ${cleanupResult.valid} remain valid."
            }
        }
    }

    private fun toByteKey(nodeId: NodeId): ByteSequence {
        return ByteSequence.from("$keyPrefix/${nodeId.namespace}/${nodeId.key}".toByteArray())
    }

    private fun fromByteKey(keyBytes: ByteSequence): NodeId {
        val keyString = keyBytes.toString(Charset.defaultCharset())

        val (_, namespace, key) = keyString.split("/")

        return NodeId(key = key, namespace = namespace)
    }
}
