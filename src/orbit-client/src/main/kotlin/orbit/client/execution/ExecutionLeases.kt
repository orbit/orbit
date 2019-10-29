/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.execution

import orbit.client.mesh.AddressableLeaser
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.util.time.Timestamp
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionLeases(
    private val addressableLeaser: AddressableLeaser
) {
    private val currentLeases = ConcurrentHashMap<AddressableReference, AddressableLease>()

    fun getLease(addressableReference: AddressableReference) = currentLeases[addressableReference]

    suspend fun getOrRenewLease(addressableReference: AddressableReference): AddressableLease {
        var currentLease = currentLeases[addressableReference]

        if (currentLease == null || currentLease.expiresAt.inPast()) {
            currentLease = renewLease(addressableReference)
        }

        return currentLease
    }

    suspend fun renewLease(addressableReference: AddressableReference): AddressableLease {
        val newLease = addressableLeaser.renewLease(addressableReference)
        checkNotNull(newLease)
        currentLeases[addressableReference] = newLease
        return newLease
    }

    suspend fun abandonLease(addressableReference: AddressableReference): Boolean {
        val currentLease = currentLeases[addressableReference]
        if (currentLease != null) {
            return addressableLeaser.abandonLease(addressableReference)
        }
        return false
    }
}