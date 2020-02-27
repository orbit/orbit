/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.runBlocking
import orbit.server.OrbitServerConfig
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.shared.exception.PlacementFailedException
import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeStatus
import orbit.util.misc.attempt
import orbit.util.time.Timestamp
import orbit.util.time.toTimestamp
import java.time.Instant

class AddressableManager(
    private val addressableDirectory: AddressableDirectory,
    private val clusterManager: ClusterManager,
    config: OrbitServerConfig
) {
    private val leaseExpiration = config.addressableLeaseDuration
    private val placementTimer = Metrics.timer("Placement Timer")

    suspend fun locateOrPlace(namespace: Namespace, addressableReference: AddressableReference): NodeId =
        addressableDirectory.getOrPut(addressableReference) {
            createNewEntry(namespace, addressableReference)
        }.let {
            val invalidNode = clusterManager.getNode(it.nodeId) == null
            val expired = Timestamp.now() > it.expiresAt
            if (invalidNode || expired) {
                val newEntry = createNewEntry(namespace, addressableReference)
                if (addressableDirectory.compareAndSet(it.reference, it, newEntry)) {
                    newEntry.nodeId
                } else {
                    locateOrPlace(namespace, addressableReference)
                }
            } else {
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

    suspend fun abandonLease(key: AddressableReference, nodeId: NodeId): Boolean {
        val currentLease = addressableDirectory.get(key)
        if (currentLease != null && currentLease.nodeId == nodeId && Timestamp.now() <= currentLease.expiresAt) {
            return addressableDirectory.compareAndSet(key, currentLease, null)
        }
        return false
    }

    private suspend fun createNewEntry(namespace: Namespace, addressableReference: AddressableReference) =
        AddressableLease(
            nodeId = place(namespace, addressableReference),
            reference = addressableReference,
            expiresAt = Instant.now().plus(leaseExpiration.expiresIn).toTimestamp(),
            renewAt = Instant.now().plus(leaseExpiration.renewIn).toTimestamp()
        )

    private suspend fun place(namespace: Namespace, addressableReference: AddressableReference): NodeId =
        runCatching {
            placementTimer.recordCallable {
                runBlocking {
                    attempt(
                        maxAttempts = 5,
                        initialDelay = 1000
                    ) {
                        val allNodes = clusterManager.getAllNodes()
                        val potentialNodes = allNodes
                            .filter { it.id.namespace == namespace }
                            .filter { it.nodeStatus == NodeStatus.ACTIVE }
                            .filter { it.capabilities.addressableTypes.contains(addressableReference.type) }

                        potentialNodes.random().id
                    }
                }
            }
        }.fold(
            { it },
            { throw PlacementFailedException("Could not find node capable of hosting $addressableReference") })
}