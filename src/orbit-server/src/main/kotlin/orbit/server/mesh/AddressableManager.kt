/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.server.OrbitServerConfig
import orbit.shared.exception.PlacementFailedException
import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.shared.remoting.AddressableReference
import orbit.util.misc.attempt
import orbit.util.time.toTimestamp
import java.time.Instant

class AddressableManager(
    private val addressableDirectory: AddressableDirectory,
    private val clusterManager: ClusterManager,
    config: OrbitServerConfig
) {
    private val leaseExpiration = config.addressableLeaseDuration

    suspend fun locateOrPlace(namespace: Namespace, addressableReference: AddressableReference): NodeId {
        return addressableDirectory.getOrPut(addressableReference) {
            AddressableLease(
                nodeId = place(namespace, addressableReference),
                reference = addressableReference,
                expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
            )

        }.nodeId
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
        }.fold(
            { it },
            { throw PlacementFailedException("Could not find node capable of hosting $addressableReference") })
    }
}