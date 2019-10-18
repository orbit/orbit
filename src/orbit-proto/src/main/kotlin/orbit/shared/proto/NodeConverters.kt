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
import orbit.shared.net.HostInfo

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
        .let {
            if (hostInfo != null) it.setHostInfo(hostInfo!!.toHostInfoProto()) else it
        }
        .build()

fun Node.NodeInfoProto.toNodeInfo(): NodeInfo =
    NodeInfo(
        id = NodeId(key = id.key, namespace = id.namespace),
        visibleNodes = visibleNodesList.map { it.toNodeId() }.toSet(),
        lease = lease.toLeaseProto(),
        capabilities = capabilities.toCapabilities(),
        hostInfo = hostInfo.toHostInfo()
    )

fun Node.HostInfoProto.toHostInfo(): HostInfo =
    HostInfo(host, port)

fun HostInfo.toHostInfoProto(): Node.HostInfoProto =
    Node.HostInfoProto.newBuilder()
        .setHost(host)
        .setPort(port)
        .build()

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