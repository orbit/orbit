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
        content = content.toMessageContent()
    )

fun Message.toMessageProto(): Messages.MessageProto =
    Messages.MessageProto.newBuilder().let {
        if (messageId != null) it.setMessageId(messageId!!) else it
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
                message = error.message,
                status = error.status.toStatus()
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
                            .setMessage(message)
                            .setStatus(status.toStatusProto())
                            .build()
                    )
                }
            }
        }.build()

fun Messages.ErrorProto.StatusProto.toStatus(): MessageContent.Error.Status =
    when (number) {
        Messages.ErrorProto.StatusProto.UNKNOWN_VALUE -> MessageContent.Error.Status.UNKNOWN
        Messages.ErrorProto.StatusProto.INVALID_LEASE_VALUE -> MessageContent.Error.Status.INVALID_LEASE
        Messages.ErrorProto.StatusProto.SERVER_OVERLOADED_VALUE -> MessageContent.Error.Status.SERVER_OVERLOADED
        Messages.ErrorProto.StatusProto.SECURITY_VIOLATION_VALUE -> MessageContent.Error.Status.SECURITY_VIOLATION
        else -> MessageContent.Error.Status.UNKNOWN
    }

fun MessageContent.Error.Status.toStatusProto(): Messages.ErrorProto.StatusProto =
    when (this) {
        MessageContent.Error.Status.UNKNOWN -> Messages.ErrorProto.StatusProto.UNKNOWN
        MessageContent.Error.Status.INVALID_LEASE -> Messages.ErrorProto.StatusProto.INVALID_LEASE
        MessageContent.Error.Status.SERVER_OVERLOADED -> Messages.ErrorProto.StatusProto.SERVER_OVERLOADED
        MessageContent.Error.Status.SECURITY_VIOLATION -> Messages.ErrorProto.StatusProto.SECURITY_VIOLATION
        else -> Messages.ErrorProto.StatusProto.UNKNOWN
    }