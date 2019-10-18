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
import orbit.util.time.now
import java.util.concurrent.ConcurrentHashMap

internal class ExecutionLeases(
    private val addressableLeaser: AddressableLeaser
) {
    private val currentLeases = ConcurrentHashMap<AddressableReference, AddressableLease>()

    suspend fun getOrRenewLease(addressableReference: AddressableReference): AddressableLease {
        var currentLease = currentLeases[addressableReference]

        if (currentLease == null || currentLease.expiresAt < Timestamp.now()) {
            currentLease = addressableLeaser.renewLease(addressableReference)
        }

        checkNotNull(currentLease)


        currentLeases[addressableReference] = currentLease

        return currentLease
    }
}