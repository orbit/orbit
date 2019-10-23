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

    private val client = Client.builder().endpoints(config.url).build().kvClient

    fun toKey(address: AddressableReference): ByteSequence {
        return ByteSequence.from("addressable/${address.type}/${address.key}".toByteArray())
    }

    fun fromKey(keyBytes: ByteSequence): AddressableReference {
        val keyString = keyBytes.toString(Charset.defaultCharset())

        val (_, type, key) = keyString.split("/")

        return AddressableReference(type, Key.of(key))
    }

    override suspend fun set(key: AddressableReference, value: AddressableLease) {
        client.put(toKey(key), ByteSequence.from(key.toAddressableReferenceProto().toByteArray())).await()
    }

    override suspend fun get(key: AddressableReference): AddressableLease? {
        val response = client.get(toKey(key)).await()
        val value = response.kvs.firstOrNull()?.value
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
