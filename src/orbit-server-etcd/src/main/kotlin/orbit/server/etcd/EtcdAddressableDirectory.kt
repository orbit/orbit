/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.etcd

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.KeyValue
import io.etcd.jetcd.op.Op
import io.etcd.jetcd.options.DeleteOption
import io.etcd.jetcd.options.GetOption
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import orbit.server.mesh.AddressableDirectory
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.proto.Addressable
import orbit.shared.proto.toAddressableLease
import orbit.shared.proto.toAddressableLeaseProto
import orbit.shared.proto.toAddressableReferenceProto
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import orbit.util.time.stopwatch
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class EtcdAddressableDirectory(config: EtcdAddressableDirectoryConfig, private val clock: Clock) :
    AddressableDirectory {

    data class EtcdAddressableDirectoryConfig(
        val url: String = System.getenv("ADDRESSABLE_DIRECTORY") ?: "0.0.0.0",
        val cleanupFrequencyRange: Pair<Duration, Duration> = Duration.ofMinutes(1) to Duration.ofMinutes(2)
    ) : ExternallyConfigured<AddressableDirectory> {
        override val instanceType: Class<out AddressableDirectory> = EtcdAddressableDirectory::class.java
    }

    private val keyPrefix = "addressable"
    private val logger = KotlinLogging.logger { }

    private val client = Client.builder().endpoints(config.url).build().kvClient
    private val lastCleanup = AtomicLong(clock.currentTime)
    private val cleanupIntervalMs =
        Random.nextLong(config.cleanupFrequencyRange.first.toMillis(), config.cleanupFrequencyRange.second.toMillis())

    private val lastHealthCheckTime = AtomicLong(0)
    private val lastHealthCheck = AtomicBoolean(false)

    override suspend fun isHealthy(): Boolean {
        if (lastHealthCheckTime.get() + 5000 > clock.currentTime) {
            return lastHealthCheck.get()
        }
        try {
            lastHealthCheckTime.set(clock.currentTime)
            withTimeout(3000) {
                entries()
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

    override suspend fun count(): Int {
        return getAllItems().count()
    }

    override suspend fun set(key: AddressableReference, value: AddressableLease) {
        client.put(toKey(key), ByteSequence.from(key.toAddressableReferenceProto().toByteArray())).await()
    }

    override suspend fun get(key: AddressableReference): AddressableLease? {
        val response = client.get(toKey(key)).await()
        return response.kvs.firstOrNull()?.value?.let {
            Addressable.AddressableLeaseProto.parseFrom(it.bytes).toAddressableLease()
        }
    }

    override suspend fun remove(key: AddressableReference): Boolean {
        client.delete(toKey(key))
        return true
    }

    override suspend fun compareAndSet(
        key: AddressableReference,
        initialValue: AddressableLease?,
        newValue: AddressableLease?
    ): Boolean {
        val byteKey = toKey(key)
        val oldValue = client.get(byteKey).await().kvs.firstOrNull()?.value?.bytes?.let {
            Addressable.AddressableLeaseProto.parseFrom(it).toAddressableLease()
        }

        if (initialValue == oldValue) {
            if (newValue != null) {
                client.put(byteKey, ByteSequence.from(newValue.toAddressableLeaseProto().toByteArray())).await()
            } else {
                client.delete(byteKey).await()
            }
            return true
        }
        return false
    }

    override suspend fun entries(): Iterable<Pair<AddressableReference, AddressableLease>> {
        return getAllItems().map { kv ->
            Pair(
                fromKey(kv.key),
                Addressable.AddressableLeaseProto.parseFrom(kv.value.bytes).toAddressableLease()
            )
        }
    }

    private suspend fun getAllItems(): MutableList<KeyValue> {
        val key = ByteSequence.from("\u0000".toByteArray())

        val option = GetOption.newBuilder()
            .withSortField(GetOption.SortTarget.KEY)
            .withSortOrder(GetOption.SortOrder.DESCEND)
            .withPrefix(ByteSequence.from(keyPrefix.toByteArray()))
            .withRange(key)
            .build()

        return client.get(key, option).await().kvs
    }

    override suspend fun tick() {
        if (lastCleanup.get() + cleanupIntervalMs < clock.currentTime) {
            logger.info { "Starting Addressable Directory cleanup..." }
            val (time, cleanupResult) = stopwatch(clock) {
                lastCleanup.set(clock.currentTime)
                val addressables = values()

                val (expiredLeases, validLeases) = addressables.partition { addressable -> clock.inPast(addressable.expiresAt) }

                if (expiredLeases.any()) {
                    val txn = client.txn()
                    txn.Then(*expiredLeases.map { addressable ->
                        Op.delete(
                            toKey(addressable.reference),
                            DeleteOption.DEFAULT
                        )
                    }.toTypedArray()).commit()
                }
                expiredLeases to validLeases
            }

            logger.info {
                "Addressable Directory cleanup took ${time}ms. Removed ${cleanupResult.first.size} entries, ${cleanupResult.second.size} remain valid."
            }
        }
    }

    private fun toKey(address: AddressableReference): ByteSequence {
        return ByteSequence.from("$keyPrefix/${address.type}/${address.key}".toByteArray())
    }

    private fun fromKey(keyBytes: ByteSequence): AddressableReference {
        val keyString = keyBytes.toString(Charset.defaultCharset())

        val (_, type, key) = keyString.split("/")

        return AddressableReference(type, Key.of(key))
    }
}
