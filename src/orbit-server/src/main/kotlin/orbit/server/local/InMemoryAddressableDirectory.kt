/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.local

import orbit.server.addressable.AddressableReference
import orbit.server.net.AddressableLease
import orbit.server.net.LeaseExpiration
import orbit.server.net.NodeId
import orbit.server.routing.AddressableDirectory
import java.time.Instant

class InMemoryAddressableDirectory(val expiration: LeaseExpiration) : AddressableDirectory {
    companion object Singleton {
        @JvmStatic
        private var directory: MutableMap<AddressableReference, AddressableLease> = HashMap()
    }

    override suspend fun getLease(address: AddressableReference): AddressableLease? {
        return directory[address]
    }

    override suspend fun setLocation(address: AddressableReference, node: NodeId) {
        directory[address] = AddressableLease(
            address = address,
            nodeId = node,
            expiresAt = Instant.now().plus(expiration.duration),
            renewAt = Instant.now().plus(expiration.renew)
        )
    }

    override suspend fun updateLease(address: AddressableReference): AddressableLease {
        var lease = directory[address]
        if (lease != null) {
            lease = lease.copy(
                expiresAt = Instant.now().plus(expiration.duration),
                renewAt = Instant.now().plus(expiration.renew)
            )
            directory[address] = lease
        }
        return lease?: AddressableLease.Empty
    }
}
