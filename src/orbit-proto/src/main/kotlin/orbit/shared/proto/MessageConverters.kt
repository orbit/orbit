/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.net.Message
import orbit.shared.net.MessageContent

fun Messages.MessageProto.toMessage(): Message =
    Message(
        messageId = messageId,
        source = source?.toNodeId(),
        content = content.toMessageContent()
    )

fun Message.toMessageProto(): Messages.MessageProto =
    Messages.MessageProto.newBuilder().let {
        if (messageId != null) it.setMessageId(messageId!!) else it
    }.let {
        if (source != null) it.setSource(source!!.toNodeIdProto()) else it
    }.setContent(content.toMessageContentProto()).build()

fun Messages.MessageContentProto.toMessageContent(): MessageContent =
    when {
        hasInvocationRequest() -> {
            MessageContent.InvocationRequest(
                data = invocationRequest.value,
                destination = invocationRequest.reference.toAddressableReference()
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
                            .setValue(data)
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

                is MessageContent.Error -> {
                    builder.setError(
                        Messages.ErrorProto.newBuilder()
                            .setDescription(description)
                            .build()
                    )
                }
            }
        }.build()
