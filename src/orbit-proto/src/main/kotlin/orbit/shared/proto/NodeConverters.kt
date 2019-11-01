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
import orbit.shared.mesh.NodeStatus

fun NodeId.toNodeIdProto(): Node.NodeIdProto =
    Node.NodeIdProto.newBuilder()
        .setKey(key)
        .setNamespace(namespace)
        .build()

fun Node.NodeIdProto.toNodeId(): NodeId =
    NodeId(
        key = key,
        namespace = namespace
    )

fun NodeInfo.toNodeInfoProto(): Node.NodeInfoProto =
    Node.NodeInfoProto.newBuilder()
        .setId(id.toNodeIdProto())
        .addAllVisibleNodes(visibleNodes.map { it.toNodeIdProto() })
        .setLease(lease.toNodeLeaseProto())
        .setCapabilities(capabilities.toCapabilitiesProto())
        .setStatus(nodeStatus.toNodeStatusProto())
        .let {
            if (url != null) it.setUrl(url) else it
        }
        .build()

fun Node.NodeInfoProto.toNodeInfo(): NodeInfo =
    NodeInfo(
        id = NodeId(key = id.key, namespace = id.namespace),
        visibleNodes = visibleNodesList.map { it.toNodeId() }.toSet(),
        lease = lease.toLeaseProto(),
        capabilities = capabilities.toCapabilities(),
        url = url,
        nodeStatus = status.toNodeStatus()
    )

fun NodeLease.toNodeLeaseProto(): Node.NodeLeaseProto =
    Node.NodeLeaseProto.newBuilder()
        .setChallengeToken(challengeToken)
        .setExpiresAt(expiresAt.toTimestampProto())
        .setRenewAt(renewAt.toTimestampProto())
        .build()

fun Node.NodeLeaseProto.toLeaseProto(): NodeLease =
    NodeLease(
        challengeToken = challengeToken,
        expiresAt = expiresAt.toTimestamp(),
        renewAt = renewAt.toTimestamp()
    )

fun NodeCapabilities.toCapabilitiesProto(): Node.CapabilitiesProto =
    Node.CapabilitiesProto.newBuilder()
        .addAllAddressableTypes(addressableTypes)
        .build()

fun Node.CapabilitiesProto.toCapabilities(): NodeCapabilities =
    NodeCapabilities(
        addressableTypes = addressableTypesList
    )

fun Node.NodeStatusProto.toNodeStatus(): NodeStatus = when (this.number) {
    Node.NodeStatusProto.ACTIVE_VALUE -> NodeStatus.ACTIVE
    Node.NodeStatusProto.STOPPED_VALUE -> NodeStatus.STOPPED
    Node.NodeStatusProto.STARTING_VALUE -> NodeStatus.STARTING
    Node.NodeStatusProto.DRAINING_VALUE -> NodeStatus.DRAINING
    else -> error("Unknown node status")
}

fun NodeStatus.toNodeStatusProto(): Node.NodeStatusProto = Node.NodeStatusProto.forNumber(
    when (this) {
        NodeStatus.ACTIVE -> Node.NodeStatusProto.ACTIVE_VALUE
        NodeStatus.STOPPED -> Node.NodeStatusProto.STOPPED_VALUE
        NodeStatus.STARTING -> Node.NodeStatusProto.STARTING_VALUE
        NodeStatus.DRAINING -> Node.NodeStatusProto.DRAINING_VALUE
    }
)
