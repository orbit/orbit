/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.etcd

import orbit.server.mesh.AddressableDirectory
import orbit.server.mesh.LeaseDuration
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.shared.mesh.NodeId
import java.time.Instant

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