/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.net.NetSystem

class DirectorySystem(
    private val addressableDirectory: AddressableDirectory,
    private val netSystem: NetSystem
) {
    suspend fun locate(addressableReference: AddressableReference): NetTarget? {
        return addressableDirectory.get(addressableReference)
    }

    suspend fun locateOrPlace(addressableReference: AddressableReference, messageTarget: NetTarget): NetTarget {
        return addressableDirectory.getOrPut(addressableReference, messageTarget)
    }

    suspend fun localActivation(addressableReference: AddressableReference) {
        addressableDirectory.put(addressableReference, NetTarget.Unicast(netSystem.localNode.nodeIdentity))
    }

    suspend fun localDeactivation(addressableReference: AddressableReference) {
        addressableDirectory.removeIf(addressableReference, NetTarget.Unicast(netSystem.localNode.nodeIdentity))
    }
}