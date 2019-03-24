/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.cluster.local.LocalAddressableDirectory

interface AddressableDirectory {
    suspend fun get(addressableReference: AddressableReference) : NetTarget?
    suspend fun getOrPut(addressableReference: AddressableReference, messageTarget: NetTarget): NetTarget
    suspend fun put(addressableReference: AddressableReference, messageTarget: NetTarget): NetTarget
    suspend fun removeIf(addressableReference: AddressableReference, messageTarget: NetTarget): Boolean
}

typealias DefaultAddressableDirectory = LocalAddressableDirectory