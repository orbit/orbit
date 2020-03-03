/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh.local

import orbit.server.mesh.AddressableDirectory
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.util.concurrent.HashMapBackedAsyncMap
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import java.util.concurrent.ConcurrentHashMap

class LocalAddressableDirectory(private val clock: Clock) :
    HashMapBackedAsyncMap<AddressableReference, AddressableLease>(),
    AddressableDirectory {
    object LocalAddressableDirectorySingleton : ExternallyConfigured<AddressableDirectory> {
        override val instanceType = LocalAddressableDirectory::class.java
    }

    override val map: ConcurrentHashMap<AddressableReference, AddressableLease>
        get() = globalMap

    companion object {
        @JvmStatic
        private val globalMap = ConcurrentHashMap<AddressableReference, AddressableLease>()

        fun clear() {
            globalMap.clear()
        }
    }

    override suspend fun isHealthy(): Boolean {
        return true
    }

    override suspend fun tick() {
        // Cull expired
        values().filter { clock.inPast(it.expiresAt) }.also { toDelete ->
            toDelete.forEach {
                remove(it.reference)
            }
        }
    }

    override suspend fun count(): Int = this.keys().count()
}