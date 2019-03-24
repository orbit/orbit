/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableReference
import java.util.concurrent.ConcurrentHashMap

class DefaultAddressableDirectory : AddressableDirectory {
    private val concurrentHashMap = ConcurrentHashMap<AddressableReference, NetTarget>()

    override suspend fun locate(addressableReference: AddressableReference): NetTarget? =
        concurrentHashMap[addressableReference]

    override suspend fun locateOrPlace(
        addressableReference: AddressableReference,
        messageTarget: NetTarget
    ): NetTarget =
        concurrentHashMap.getOrPut(addressableReference) {
            messageTarget
        }
}