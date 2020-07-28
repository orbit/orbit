/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh.local

import orbit.server.mesh.AddressableDirectory
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.NamespacedAddressableReference
import orbit.util.concurrent.HashMapBackedAsyncMap
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import java.util.concurrent.ConcurrentHashMap

class LocalAddressableDirectory(private val clock: Clock) :
    HashMapBackedAsyncMap<NamespacedAddressableReference, AddressableLease>(),
    AddressableDirectory {
    object LocalAddressableDirectorySingleton : ExternallyConfigured<AddressableDirectory> {
        override val instanceType = LocalAddressableDirectory::class.java
    }

    override val map: ConcurrentHashMap<NamespacedAddressableReference, AddressableLease>
        get() = globalMap

    companion object {
        @JvmStatic
        private val globalMap = ConcurrentHashMap<NamespacedAddressableReference, AddressableLease>()

        fun clear() {
            globalMap.clear()
        }
    }

    override suspend fun isHealthy(): Boolean {
        return true
    }

    override suspend fun tick() {
        // Cull expired
        globalMap.values.filter { clock.inPast(it.expiresAt) }.also { toDelete ->
            toDelete.forEach {
                remove(NamespacedAddressableReference(it.nodeId.namespace, it.reference))
            }
        }
    }

    override suspend fun count() = globalMap.count().toLong()
}