/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

interface AddressableConstructor {
    fun constructAddressable(clazz: Class<out Addressable>): Addressable
}