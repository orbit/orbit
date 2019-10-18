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
import orbit.server.mesh.AddressableDirectory
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.proto.Addressable
import orbit.shared.proto.toAddressableLease
import orbit.shared.proto.toAddressableLeaseProto
import orbit.shared.proto.toAddressableReferenceProto
import orbit.util.di.jvm.ExternallyConfigured
import java.nio.charset.Charset

class EtcdAddressableDirectory(config: EtcdAddressableDirectoryConfig) : AddressableDirectory {
    data class EtcdAddressableDirectoryConfig(
        val url: String
    ) : ExternallyConfigured<AddressableDirectory> {
        override val instanceType: Class<out AddressableDirectory> = EtcdAddressableDirectory::class.java
    }

    init {
        println("Starting etcd addressable directory at ${config.url}")
    }

    private val client = Client.builder().endpoints(config.url).build().kvClient

    fun toKey(address: AddressableReference): ByteSequence {
        return ByteSequence.from("addressable/${address.type}/${address.key}".toByteArray())
    }

    fun fromKey(key: ByteSequence): AddressableReference {
        val keyString = key.toString(Charset.defaultCharset())

        val (_, type, key) = keyString.split("/")

        return AddressableReference(type, Key.of(key))
    }

    override suspend fun set(key: AddressableReference, value: AddressableLease) {
        println("set val ${key}: ${value}")
        client.put(toKey(key), ByteSequence.from(key.toAddressableReferenceProto().toByteArray())).await()
    }

    override suspend fun get(key: AddressableReference): AddressableLease? {
        val response = client.get(toKey(key)).await()
        val value = response.kvs.firstOrNull()?.value

        println("get val ${key}: ${value}")

        return if (value != null) Addressable.AddressableLeaseProto.parseFrom(value.bytes).toAddressableLease() else null
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
            if (it != null) Addressable.AddressableLeaseProto.parseFrom(it).toAddressableLease() else null
        }

        println("compare and set ${key}: (i-${initialValue}, o-${oldValue}) -> ${newValue}")

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
                Addressable.AddressableLeaseProto.parseFrom(kv.value.bytes).toAddressableLease()
            )
        }
    }

}

/*
class EtcdAddressableDirectory(private val config: EtcdAddressableDirectoryConfig) : AddressableDirectory {
    data class EtcdAddressableDirectoryConfig(
        val url: String,
        val expiration: LeaseDuration
    ) :
        InjectedWithConfig<AddressableDirectory> {
        override val instanceType: Class<out AddressableDirectory> = EtcdAddressableDirectory::class.java
    }

    private val client = Client.builder().endpoints(config.url).build().kvClient

    fun getKey(address: AddressableReference): ByteSequence {
        val key = "${address.type}-${address.id}"
        return ByteSequence.from(key.toByteArray())
    }

    override suspend fun getLease(address: AddressableReference): AddressableLease? {
        val response = client.get(getKey(address)).await()
        val value = response.kvs.firstOrNull()?.value
        println("get node $address = $value")

        return if (value != null) AddressableLease.fromProto(AddressableLease.parseFrom(value.bytes)) else null
    }

    override suspend fun setLocation(address: AddressableReference, nodeId: NodeId) {
        val lease = AddressableLease(
            address = address,
            nodeId = nodeId,
            expiresAt = Instant.now().plus(config.expiration.duration),
            renewAt = Instant.now().plus(config.expiration.renew)
        )

        client.put(getKey(address), ByteSequence.from(lease.toProto().toByteArray())).await()
    }

    override suspend fun updateLease(address: AddressableReference): AddressableLease {
        val lease = this.getLease(address)!!
        val newLease = lease.copy(
            expiresAt = Instant.now().plus(config.expiration.duration),
            renewAt = Instant.now().plus(config.expiration.renew)
        )
        client.put(
            getKey(address),
            ByteSequence.from(
                newLease.toProto().toByteArray()
            )
        ).await()

        return newLease
    }
}

 */