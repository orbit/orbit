/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.hosting.AddressableDirectory
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.net.NetSystem

internal class DirectorySystem(
    private val addressableDirectory: AddressableDirectory,
    private val netSystem: NetSystem
) {
    suspend fun locate(addressableReference: AddressableReference): NetTarget? {
        return addressableDirectory.get(addressableReference)
    }

    suspend fun locateOrPlace(addressableReference: AddressableReference, messageTarget: NetTarget): NetTarget {
        return addressableDirectory.getOrPut(addressableReference, messageTarget)
    }

    suspend fun forcePlaceLocal(addressableReference: AddressableReference) {
        addressableDirectory.put(addressableReference, netSystem.localNode.nodeIdentity.asTarget())
    }

    suspend fun removeIfLocal(addressableReference: AddressableReference) {
        addressableDirectory.removeIf(addressableReference, netSystem.localNode.nodeIdentity.asTarget())
    }
}