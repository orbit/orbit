/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.mesh.NodeId
import orbit.shared.net.InvocationReason
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget
import orbit.shared.router.Route

fun Messages.MessageProto.toMessage(): Message =
    Message(
        messageId = messageId,
        source = source?.toNodeId(),
        target = target?.toMessageTarget(),
        content = content.toMessageContent()
    )

fun Message.toMessageProto(): Messages.MessageProto =
    Messages.MessageProto.newBuilder().let {
        if (messageId != null) it.setMessageId(messageId!!) else it
    }.let {
        if (source != null) it.setSource(source!!.toNodeIdProto()) else it
    }.let {
        if (target != null) it.setTarget(target!!.toMessageTargetProto()) else it
    }.setContent(content.toMessageContentProto()).build()

fun Messages.MessageTargetProto.toMessageTarget(): MessageTarget? =
    when (this.targetCase.number) {
        Messages.MessageTargetProto.UNICASTTARGET_FIELD_NUMBER -> {
            MessageTarget.Unicast(unicastTarget.target.toNodeId())
        }
        Messages.MessageTargetProto.ROUTEDUNICASTTARGET_FIELD_NUMBER -> {
            MessageTarget.RoutedUnicast(Route(routedUnicastTarget.targetList.map { it.toNodeId() }))
        }
        else -> null
    }

fun MessageTarget.toMessageTargetProto() = Messages.MessageTargetProto.newBuilder().let {
    when (this) {
        is MessageTarget.Unicast -> it.setUnicastTarget(
            Messages.MessageTargetProto.Unicast.newBuilder().setTarget(targetNode.toNodeIdProto())
        )
        is MessageTarget.RoutedUnicast -> it.setRoutedUnicastTarget(
            Messages.MessageTargetProto.RoutedUnicast.newBuilder()
                .addAllTarget(route.path.map(NodeId::toNodeIdProto))
        )
    }
}.build()


fun Messages.MessageContentProto.toMessageContent(): MessageContent =
    when {
        hasInvocationRequest() -> {
            MessageContent.InvocationRequest(
                method = invocationRequest.method,
                arguments = invocationRequest.arguments,
                destination = invocationRequest.reference.toAddressableReference(),
                reason = InvocationReason.fromInt(invocationRequest.reasonValue)
            )
        }

        hasInvocationResponse() -> {
            MessageContent.InvocationResponse(
                data = invocationResponse.value
            )
        }

        hasError() -> {
            MessageContent.Error(
                description = error.description
            )
        }

        hasInvocationResponseError() -> {
            MessageContent.InvocationResponseError(
                description = invocationResponseError.description,
                platform = invocationResponseError.platform

            )
        }

        hasInfoRequest() -> {
            MessageContent.ConnectionInfoRequest
        }

        hasInfoResponse() -> {
            MessageContent.ConnectionInfoResponse(
                nodeId = infoResponse.nodeId.toNodeId()
            )
        }

        else -> throw Throwable("Unknown message type")
    }

fun MessageContent.toMessageContentProto(): Messages.MessageContentProto =
    Messages.MessageContentProto.newBuilder()
        .let { builder ->
            when (this) {
                is MessageContent.InvocationRequest -> {
                    builder.setInvocationRequest(
                        Messages.InvocationRequestProto.newBuilder()
                            .setReference(destination.toAddressableReferenceProto())
                            .setMethod(method)
                            .setArguments(arguments)
                            .setReason(Messages.InvocationReasonProto.forNumber(reason.value))
                            .build()
                    )
                }

                is MessageContent.InvocationResponse -> {
                    builder.setInvocationResponse(
                        Messages.InvocationResponseProto.newBuilder()
                            .setValue(data)
                            .build()
                    )
                }

                is MessageContent.InvocationResponseError -> {
                    builder.setInvocationResponseError(
                        Messages.InvocationResponseErrorProto.newBuilder()
                            .setDescription(description)
                            .setPlatform(platform)
                            .build()
                    )
                }

                is MessageContent.Error -> {
                    builder.setError(
                        Messages.ErrorProto.newBuilder()
                            .setDescription(description)
                            .build()
                    )
                }

                is MessageContent.ConnectionInfoRequest -> {
                    builder.setInfoRequest(
                        Messages.ConnectionInfoRequestProto.newBuilder()
                    )
                }

                is MessageContent.ConnectionInfoResponse -> {
                    builder.setInfoResponse(
                        Messages.ConnectionInfoResponseProto.newBuilder()
                            .setNodeId(nodeId.toNodeIdProto())
                    )
                }

            }
        }.build()
