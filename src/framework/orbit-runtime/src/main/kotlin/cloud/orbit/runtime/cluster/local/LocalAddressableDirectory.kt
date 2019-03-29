/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.cluster.local

import cloud.orbit.core.hosting.AddressableDirectory
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableReference
import java.util.concurrent.ConcurrentHashMap

class LocalAddressableDirectory : AddressableDirectory {
    private val concurrentHashMap = ConcurrentHashMap<AddressableReference, NetTarget>()

    override suspend fun get(addressableReference: AddressableReference): NetTarget? =
        concurrentHashMap[addressableReference]

    override suspend fun getOrPut(
        addressableReference: AddressableReference,
        messageTarget: NetTarget
    ): NetTarget =
        concurrentHashMap.getOrPut(addressableReference) {
            messageTarget
        }

    override suspend fun put(addressableReference: AddressableReference, messageTarget: NetTarget): NetTarget =
        concurrentHashMap.put(addressableReference, messageTarget)!!

    override suspend fun removeIf(addressableReference: AddressableReference, messageTarget: NetTarget): Boolean =
        concurrentHashMap.remove(addressableReference, messageTarget)

}