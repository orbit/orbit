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

fun NodeId.toNodeIdProto(): Mesh.NodeIdProto =
    Mesh.NodeIdProto.newBuilder()
        .setKey(key)
        .setNamespace(namespace)
        .build()

fun Mesh.NodeIdProto.toNodeId(): NodeId =
    NodeId(
        key = key,
        namespace = namespace
    )

fun NodeInfo.toNodeInfoProto(): Mesh.NodeInfoProto =
    Mesh.NodeInfoProto.newBuilder()
        .setId(id.toNodeIdProto())
        .addAllVisibleNodes(visibleNodes.map { it.toNodeIdProto() })
        .setLease(lease.toNodeLeaseProto())
        .setCapabilities(capabilities.toCapabilitiesProto())
        .build()

fun Mesh.NodeInfoProto.toNodeInfo(): NodeInfo =
    NodeInfo(
        id = NodeId(key = id.key, namespace = id.namespace),
        visibleNodes = visibleNodesList.map { it.toNodeId() }.toSet(),
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