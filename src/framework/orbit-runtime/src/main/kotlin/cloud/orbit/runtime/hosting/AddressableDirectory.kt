/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.AddressableReference

interface AddressableDirectory {
    suspend fun locate(addressableReference: AddressableReference) : NetTarget?
    suspend fun locateOrPlace(addressableReference: AddressableReference, messageTarget: NetTarget): NetTarget
}