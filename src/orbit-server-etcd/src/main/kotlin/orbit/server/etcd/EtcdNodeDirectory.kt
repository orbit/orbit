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
import io.etcd.jetcd.options.PutOption
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import orbit.server.mesh.NodeDirectory
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.proto.Node
import orbit.shared.proto.toAddressableLeaseProto
import orbit.shared.proto.toNodeInfo
import orbit.shared.proto.toNodeInfoProto
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import orbit.util.time.Timestamp
import orbit.util.time.stopwatch
import orbit.util.time.toInstant
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
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

    private val client = Client.builder().endpoints(config.url).build()
    private val kvClient = client.kvClient
    private val leaseClient = client.leaseClient

    private val lastHealthCheckTime = AtomicLong(0)
    private val lastHealthCheck = AtomicBoolean(false)

    override suspend fun isHealthy(): Boolean {
        if (lastHealthCheckTime.get() + 5000 > clock.currentTime) {
            return lastHealthCheck.get()
        }
        try {
            lastHealthCheckTime.set(clock.currentTime)
            withTimeout(3000) {
                getLease(Timestamp.now())
            }
            lastHealthCheck.set(true)
            return true
        } catch (e: TimeoutCancellationException) {
            lastHealthCheck.set(false)
            return false
        } catch (e: ExecutionException) {
            lastHealthCheck.set(false)
            return false
        }
    }

    suspend fun getLease(time: Timestamp): PutOption {
        val lease = leaseClient.grant(clock.until(time).seconds).await()
        return PutOption.newBuilder().withLeaseId(lease.id).build()
    }

    override suspend fun get(key: NodeId): NodeInfo? {
        val response = kvClient.get(toByteKey(key)).await()
        return response.kvs.firstOrNull()?.value?.let {
            Node.NodeInfoProto.parseFrom(it.bytes).toNodeInfo()
        }
    }

    override suspend fun remove(key: NodeId): Boolean {
        kvClient.delete(toByteKey(key)).await()
        return true
    }

    override suspend fun compareAndSet(key: NodeId, initialValue: NodeInfo?, newValue: NodeInfo?): Boolean {
        val byteKey = toByteKey(key)
        val oldValue = kvClient.get(byteKey).await().kvs.firstOrNull()?.value?.bytes?.let {
            Node.NodeInfoProto.parseFrom(it).toNodeInfo()
        }

        if (initialValue == oldValue) {
            if (newValue != null) {
                kvClient.put(
                    byteKey,
                    ByteSequence.from(newValue.toNodeInfoProto().toByteArray()),
                    getLease(newValue.lease.expiresAt)
                ).await()
            } else {
                kvClient.delete(byteKey).await()
            }
            return true
        }
        return false
    }

    override suspend fun count() =
        kvClient.get(
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

        val response = kvClient.get(allKey, option).await()

        return response.kvs.map { kv ->
            Pair(
                fromByteKey(kv.key),
                Node.NodeInfoProto.parseFrom(kv.value.bytes).toNodeInfo()
            )
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
