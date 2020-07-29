/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.etcd

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.options.GetOption
import io.etcd.jetcd.options.PutOption
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import orbit.server.mesh.AddressableDirectory
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.NamespacedAddressableReference
import orbit.shared.proto.Addressable
import orbit.shared.proto.toAddressableLease
import orbit.shared.proto.toAddressableLeaseProto
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import orbit.util.time.Timestamp
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class EtcdAddressableDirectory(config: EtcdAddressableDirectoryConfig, private val clock: Clock) :
    AddressableDirectory {

    data class EtcdAddressableDirectoryConfig(
        val url: String = System.getenv("ADDRESSABLE_DIRECTORY") ?: "0.0.0.0"
    ) : ExternallyConfigured<AddressableDirectory> {
        override val instanceType: Class<out AddressableDirectory> = EtcdAddressableDirectory::class.java
    }

    private val keyPrefix = "addressable"
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

    suspend fun getLease(time: Timestamp): PutOption {
        val lease = leaseClient.grant(clock.until(time).seconds).await()
        return PutOption.newBuilder().withLeaseId(lease.id).build()
    }

    override suspend fun get(key: NamespacedAddressableReference): AddressableLease? {
        val response = kvClient.get(toByteKey(key)).await()
        return response.kvs.firstOrNull()?.value?.let {
            Addressable.AddressableLeaseProto.parseFrom(it.bytes).toAddressableLease()
        }
    }

    override suspend fun remove(key: NamespacedAddressableReference): Boolean {
        kvClient.delete(toByteKey(key))
        return true
    }

    override suspend fun compareAndSet(
        key: NamespacedAddressableReference,
        initialValue: AddressableLease?,
        newValue: AddressableLease?
    ): Boolean {
        val byteKey = toByteKey(key)
        val entry = kvClient.get(byteKey).await().kvs.firstOrNull()

        val oldValue = entry?.value?.bytes?.let {
            Addressable.AddressableLeaseProto.parseFrom(it).toAddressableLease()
        }

        if (initialValue == oldValue) {
            if (newValue != null) {
                kvClient.put(
                    byteKey,
                    ByteSequence.from(newValue.toAddressableLeaseProto().toByteArray()),
                    getLease(newValue.expiresAt)
                ).await()
            } else {
                kvClient.delete(byteKey).await()
            }
            return true
        }
        return false
    }

    private fun toByteKey(reference: NamespacedAddressableReference): ByteSequence {
        return ByteSequence.from("$keyPrefix/${reference.namespace}/${reference.addressableReference.type}/${reference.addressableReference.key}".toByteArray())
    }
}
