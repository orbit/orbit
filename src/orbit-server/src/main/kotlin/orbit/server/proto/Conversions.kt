/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.proto

import com.google.protobuf.Timestamp
import orbit.server.net.Message
import orbit.server.net.MessageContent
import orbit.server.net.NodeId
import orbit.server.net.NodeLease
import orbit.server.routing.NodeInfo
import orbit.shared.proto.Addressable
import orbit.shared.proto.Messages
import orbit.shared.proto.NodeManagementOuterClass
import java.time.Instant

fun NodeLease.toProto(): NodeManagementOuterClass.NodeLease {
    return NodeManagementOuterClass.NodeLease.newBuilder()
        .setNodeIdentity(this.nodeId.value)
        .setChallengeToken(this.challengeToken)
        .setRenewAt(Timestamp.newBuilder().setSeconds(this.renewAt.epochSecond))
        .setExpiresAt(Timestamp.newBuilder().setSeconds(this.expiresAt.epochSecond))
        .build()
}

fun NodeLease.Companion.fromProto(lease: NodeManagementOuterClass.NodeLease): NodeLease {
    return NodeLease(
        nodeId = NodeId(lease.nodeIdentity),
        challengeToken = lease.challengeToken,
        expiresAt = Instant.ofEpochSecond(lease.expiresAt.seconds),
        renewAt = Instant.ofEpochSecond(lease.renewAt.seconds)
    )
}

internal fun Message.toProto(): Messages.Message {
    val builder = Messages.Message.newBuilder()
    return when {
        this.content is MessageContent.Request ->
            builder.setInvocationRequest(
                builder.invocationRequestBuilder
                    .setValue(this.content.data)
                    .setReference(
                        Addressable.AddressableReference.newBuilder()
                            .setId(this.content.destination.id)
                            .setType(this.content.destination.type).build()
                    )
            ).build()
        else -> throw IllegalArgumentException("Message content is invalid type")
    }
}

fun NodeInfo.toProto(): NodeManagementOuterClass.NodeInfo {
    val builder = NodeManagementOuterClass.NodeInfo.newBuilder()

    when (this) {
        is NodeInfo.ClientNodeInfo ->
            builder.setClient(
                builder.clientBuilder
                    .setId(this.id.value)
                    .setLease(this.lease.toProto())
                    .setCapabilities(builder.clientBuilder.capabilitiesBuilder.addAllAddressableTypes(this.capabilities.addressableTypes).build())
                    .addAllVisibleNodes(this.visibleNodes.map { node -> node.value })
            )
        is NodeInfo.ServerNodeInfo ->
            builder.setServer(
                builder.serverBuilder
                    .setId(this.id.value)
                    .setLease(this.lease.toProto())
                    .setCapabilities(builder.serverBuilder.capabilitiesBuilder.addAllAddressableTypes(this.capabilities.addressableTypes).build())
                    .addAllVisibleNodes(this.visibleNodes.map { node -> node.value })
                    .setPort(this.port)
                    .setHost(this.host)
            )
    }
    return builder.build()
}

fun NodeInfo.Companion.fromProto(nodeInfo: NodeManagementOuterClass.NodeInfo): NodeInfo {
    if (nodeInfo.hasClient()) {
        return NodeInfo.ClientNodeInfo(
            id = NodeId(nodeInfo.client.id),
            lease = NodeLease.fromProto(nodeInfo.client.lease)
        )
    }
    if (nodeInfo.hasServer()) {
        return NodeInfo.ServerNodeInfo(
            id = NodeId(nodeInfo.server.id),
            lease = NodeLease.fromProto(nodeInfo.server.lease),
            host = nodeInfo.server.host,
            port = nodeInfo.server.port
        )
    }
    throw IllegalArgumentException("Protobuf has neither client nor server node info")
}
