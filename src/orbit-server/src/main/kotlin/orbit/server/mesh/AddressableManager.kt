/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import io.micrometer.core.instrument.Metrics
import orbit.server.OrbitServerConfig
import orbit.server.service.Meters
import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.NamespacedAddressableReference
import orbit.shared.exception.PlacementFailedException
import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeStatus
import orbit.util.instrumentation.recordSuspended
import orbit.util.misc.attempt
import orbit.util.time.Clock
import orbit.util.time.toTimestamp

class AddressableManager(
    private val addressableDirectory: AddressableDirectory,
    private val clusterManager: ClusterManager,
    private val clock: Clock,
    config: OrbitServerConfig
) {
    private val leaseExpiration = config.addressableLeaseDuration
    private val placementTimer = Metrics.timer(Meters.Names.PlacementTimer)

    suspend fun locateOrPlace(namespace: Namespace, addressableReference: AddressableReference): NodeId =
        NamespacedAddressableReference(namespace, addressableReference).let { key ->
            addressableDirectory.getOrPut(key) {
                createNewEntry(key)
            }.let {
                val invalidNode = clusterManager.getNode(it.nodeId) == null
                val expired = clock.inPast(it.expiresAt)
                if (invalidNode || expired) {
                    val newEntry = createNewEntry(key)
                    if (addressableDirectory.compareAndSet(key, it, newEntry)) {
                        newEntry.nodeId
                    } else {
                        locateOrPlace(namespace, addressableReference)
                    }
                } else {
                    it.nodeId
                }
            }
        }

    // TODO: We need to take the expiry time
    suspend fun renewLease(addressableReference: AddressableReference, nodeId: NodeId): AddressableLease =
        addressableDirectory.manipulate(
            NamespacedAddressableReference(
                nodeId.namespace,
                addressableReference
            )
        ) { initialValue ->
            if (initialValue == null || initialValue.nodeId != nodeId || clock.inPast(initialValue.expiresAt)) {
                throw PlacementFailedException("Could not renew lease for $addressableReference")
            }

            initialValue.copy(
                expiresAt = clock.now().plus(leaseExpiration.expiresIn).toTimestamp(),
                renewAt = clock.now().plus(leaseExpiration.renewIn).toTimestamp()
            )
        }!!

    suspend fun abandonLease(addressableReference: AddressableReference, nodeId: NodeId): Boolean {
        val key = NamespacedAddressableReference(nodeId.namespace, addressableReference)
        val currentLease = addressableDirectory.get(key)
        if (currentLease != null && currentLease.nodeId == nodeId && clock.nowOrPast(currentLease.expiresAt)) {
            return addressableDirectory.compareAndSet(key, currentLease, null)
        }
        return false
    }

    private suspend fun createNewEntry(reference: NamespacedAddressableReference) =
        AddressableLease(
            nodeId = place(reference),
            reference = reference.addressableReference,
            expiresAt = clock.now().plus(leaseExpiration.expiresIn).toTimestamp(),
            renewAt = clock.now().plus(leaseExpiration.renewIn).toTimestamp()
        )

    private suspend fun place(reference: NamespacedAddressableReference): NodeId =
        runCatching {
            placementTimer.recordSuspended {
                attempt(
                    maxAttempts = 5,
                    initialDelay = 1000
                ) {
                    val allNodes = clusterManager.getAllNodes()
                    val potentialNodes = allNodes
                        .filter { it.id.namespace == reference.namespace }
                        .filter { it.nodeStatus == NodeStatus.ACTIVE }
                        .filter { it.capabilities.addressableTypes.contains(reference.addressableReference.type) }

                    potentialNodes.random().id
                }
            }
        }.fold(
            { it },
            { throw PlacementFailedException("Could not find node capable of hosting $reference") })
}