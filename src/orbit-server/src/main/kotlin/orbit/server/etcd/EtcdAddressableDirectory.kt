/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.etcd

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import kotlinx.coroutines.future.await
import orbit.server.addressable.AddressableReference
import orbit.server.config.InjectedWithConfig
import orbit.server.net.AddressableLease
import orbit.server.net.LeaseExpiration
import orbit.server.net.NodeId
import orbit.server.proto.fromProto
import orbit.server.proto.toProto
import orbit.server.routing.AddressableDirectory
import orbit.shared.proto.Addressable
import java.time.Instant

class EtcdAddressableDirectory(private val config: EtcdAddressableDirectoryConfig) : AddressableDirectory {
    data class EtcdAddressableDirectoryConfig(
        val url: String,
        val expiration: LeaseExpiration
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

        return if (value != null) AddressableLease.fromProto(Addressable.AddressableLease.parseFrom(value.bytes)) else null
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
