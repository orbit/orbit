/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh.local

import orbit.server.mesh.AddressableDirectory
import orbit.shared.mesh.AddressableLease
import orbit.shared.mesh.AddressableReference
import orbit.util.concurrent.jvm.HashMapBackedAsyncMap
import orbit.util.di.jvm.ExternallyConfigured

class LocalAddressableDirectory : HashMapBackedAsyncMap<AddressableReference, AddressableLease>(),
    AddressableDirectory {
    object LocalAddressableDirectoryConfig : ExternallyConfigured<AddressableDirectory> {
        override val instanceType = LocalAddressableDirectory::class.java
    }

}