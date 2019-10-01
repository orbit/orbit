/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.common.exception.OrbitException
import orbit.shared.net.Message

fun Messages.MessageProto.toMessage(): Message =
    when {
        hasInvocationRequest() -> {
            Message.InvocationRequest(
                data = invocationRequest.value,
                destination = invocationRequest.reference.toAddressableReference()
            )
        }

        hasInvocationResponse() -> {
            Message.InvocationResponse(
                data = invocationResponse.value
            )
        }

        hasInvocationError() -> {
            Message.InvocationError(
                message = invocationError.message,
                status = invocationError.status.toStatus()
            )
        }

        else -> throw OrbitException("Unknown message type")
    }

fun Message.toMessageProto(): Messages.MessageProto =
    Messages.MessageProto.newBuilder()
        .let { builder ->
            when (this) {
                is Message.InvocationRequest -> {
                    builder.setInvocationRequest(
                        Messages.InvocationRequestProto.newBuilder()
                            .setReference(destination.toAddressableReferenceProto())
                            .setValue(data)
                            .build()
                    )
                }

                is Message.InvocationResponse -> {
                    builder.setInvocationResponse(
                        Messages.InvocationResponseProto.newBuilder()
                            .setValue(data)
                            .build()
                    )
                }

                is Message.InvocationError -> {
                    builder.setInvocationError(
                        Messages.InvocationErrorProto.newBuilder()
                            .setMessage(message)
                            .setStatus(status.toStatusProto())
                            .build()
                    )
                }
            }
        }.build()

fun Messages.InvocationErrorProto.StatusProto.toStatus(): Message.InvocationError.Status =
    when (number) {
        Messages.InvocationErrorProto.StatusProto.UNAUTHENTICATED_VALUE -> Message.InvocationError.Status.UNAUTHENTICATED
        Messages.InvocationErrorProto.StatusProto.UNAUTHORIZED_VALUE -> Message.InvocationError.Status.UNAUTHORIZED
        Messages.InvocationErrorProto.StatusProto.UNSENT_VALUE -> Message.InvocationError.Status.UNSENT
        Messages.InvocationErrorProto.StatusProto.UNKNOWN_VALUE -> Message.InvocationError.Status.UNKNOWN
        else -> Message.InvocationError.Status.UNKNOWN
    }

fun Message.InvocationError.Status.toStatusProto(): Messages.InvocationErrorProto.StatusProto =
    when (this) {
        Message.InvocationError.Status.UNAUTHENTICATED -> Messages.InvocationErrorProto.StatusProto.UNAUTHENTICATED
        Message.InvocationError.Status.UNAUTHORIZED -> Messages.InvocationErrorProto.StatusProto.UNAUTHORIZED
        Message.InvocationError.Status.UNSENT -> Messages.InvocationErrorProto.StatusProto.UNSENT
        Message.InvocationError.Status.UNKNOWN -> Messages.InvocationErrorProto.StatusProto.UNKNOWN
        else -> Messages.InvocationErrorProto.StatusProto.UNKNOWN
    }