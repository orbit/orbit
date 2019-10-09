/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.shared.remoting.AddressableReference
import orbit.util.misc.attempt

class AddressableManager(
    private val addressableDirectory: AddressableDirectory,
    private val clusterManager: ClusterManager
) {
    suspend fun placeOrLocate(namespace: Namespace, addressableReference: AddressableReference): NodeId {
        return place(namespace, addressableReference)
    }

    private suspend fun place(namespace: Namespace, addressableReference: AddressableReference): NodeId {
        return runCatching {
            attempt(
                maxAttempts = 5,
                initialDelay = 1000
            ) {
                val potentialNodes = clusterManager.allNodes
                    .filter { it.id.namespace == namespace }
                    .filter { it.capabilities.addressableTypes.contains(addressableReference.type) }

                potentialNodes.random().id
            }
        }.fold({ it }, { throw Exception("Could not find node capable of hosting $addressableReference") })
    }
}