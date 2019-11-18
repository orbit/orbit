/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import orbit.util.di.ExternallyConfigured

internal class DefaultAddressableConstructor : AddressableConstructor {
    object DefaultAddressableConstructorSingleton : ExternallyConfigured<AddressableConstructor> {
        override val instanceType = DefaultAddressableConstructor::class.java
    }

    override fun constructAddressable(clazz: Class<out Addressable>): Addressable =
        clazz.getDeclaredConstructor().newInstance()
}