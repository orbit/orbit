/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeLease

fun NodeInfo.toNodeInfoProto(): Mesh.NodeInfoProto =
    Mesh.NodeInfoProto.newBuilder()
        .setId(id.value)
        .setNamespace(namespace)
        .addAllVisibleNodes(visibleNodes.map { it.value })
        .setLease(lease.toNodeLeaseProto())
        .setCapabilities(capabilities.toCapabilitiesProto())
        .build()

fun Mesh.NodeInfoProto.toNodeInfo(): NodeInfo =
    NodeInfo(
        id = NodeId(id),
        namespace = namespace,
        visibleNodes = visibleNodesList.map { NodeId(it) }.toSet(),
        lease = lease.toLeaseProto(),
        capabilities = capabilities.toCapabilities()
    )

fun NodeLease.toNodeLeaseProto(): Mesh.NodeLeaseProto =
    Mesh.NodeLeaseProto.newBuilder()
        .setChallengeToken(challengeToken)
        .setExpiresAt(expiresAt.toTimestampProto())
        .setRenewAt(renewAt.toTimestampProto())
        .build()

fun Mesh.NodeLeaseProto.toLeaseProto(): NodeLease =
    NodeLease(
        challengeToken = challengeToken,
        expiresAt = expiresAt.toTimestamp(),
        renewAt = renewAt.toTimestamp()
    )

fun NodeCapabilities.toCapabilitiesProto(): Mesh.CapabilitiesProto =
    Mesh.CapabilitiesProto.newBuilder()
        .addAllAddressableTypes(addressableTypes)
        .build()

fun Mesh.CapabilitiesProto.toCapabilities(): NodeCapabilities =
    NodeCapabilities(
        addressableTypes = addressableTypesList
    )