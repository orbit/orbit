/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.server.OrbitServerConfig
import orbit.shared.exception.PlacementFailedException
import orbit.shared.mesh.AddressableLease
import orbit.shared.mesh.AddressableReference
import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.util.misc.attempt
import orbit.util.time.Timestamp
import orbit.util.time.now
import orbit.util.time.toTimestamp
import java.time.Instant

class AddressableManager(
    private val addressableDirectory: AddressableDirectory,
    private val clusterManager: ClusterManager,
    config: OrbitServerConfig
) {
    private val leaseExpiration = config.addressableLeaseDuration

    suspend fun locateOrPlace(namespace: Namespace, addressableReference: AddressableReference): NodeId =
        addressableDirectory.getOrPut(addressableReference) {
            createNewEntry(namespace, addressableReference)
        }.let {
            val invalidNode = clusterManager.getNode(it.nodeId) == null
            val expired = Timestamp.now() > it.expiresAt
            if(invalidNode || expired) {
                val newEntry = createNewEntry(namespace, addressableReference)
                if(addressableDirectory.compareAndSet(it.reference, it, newEntry)) {
                    newEntry.nodeId
                } else {
                    locateOrPlace(namespace, addressableReference)
                }
            }else {
                it.nodeId
            }
        }

    // TODO: We need to take the expiry time
    suspend fun renewLease(addressableReference: AddressableReference, nodeId: NodeId): AddressableLease =
        addressableDirectory.manipulate(addressableReference) { initialValue ->
            if (initialValue == null || initialValue.nodeId != nodeId || Timestamp.now() > initialValue.expiresAt) {
                throw PlacementFailedException("Could not renew lease for $addressableReference")
            }

            initialValue.copy(
                expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
            )
        }!!

    private suspend fun createNewEntry(namespace: Namespace, addressableReference: AddressableReference) =
        AddressableLease(
            nodeId = place(namespace, addressableReference),
            reference = addressableReference,
            expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
            renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
        )

    private suspend fun place(namespace: Namespace, addressableReference: AddressableReference): NodeId =
        runCatching {
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