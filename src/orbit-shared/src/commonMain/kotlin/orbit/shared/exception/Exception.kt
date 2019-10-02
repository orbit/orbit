/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.exception

import orbit.shared.mesh.ChallengeToken
import orbit.shared.mesh.NodeId
import orbit.shared.net.MessageContent

/**
 * An exception of this type is thrown when a node id is invalid.
 */
class InvalidNodeId(nodeId: NodeId) : Throwable("$nodeId is not valid. Did the lease expire?")

/**
 * An exception of this type is thrown when a lease renewal failed due to an invalid challenge token.
 */
@Suppress("UNUSED_PARAMETER")
class InvalidChallengeException(nodeId: NodeId, challengeToken: ChallengeToken) :
    Throwable("Invalid challenge for $nodeId")

/**
 * An exception of this type is thrown when an internal capacity in Orbit is exceeded.
 */
class CapacityExceededException(message: String) : Throwable(message)


fun Throwable?.toErrorContent(): MessageContent.Error = when (this) {
    is InvalidNodeId -> MessageContent.Error(
        status = MessageContent.Error.Status.INVALID_LEASE,
        message = message
    )

    is CapacityExceededException -> MessageContent.Error(
        status = MessageContent.Error.Status.SERVER_OVERLOADED,
        message = message
    )

    is InvalidChallengeException -> MessageContent.Error(
        status = MessageContent.Error.Status.SECURITY_VIOLATION,
        message = message
    )

    else -> MessageContent.Error(
        status = MessageContent.Error.Status.UNKNOWN,
        message = this?.message ?: "Unknown error"
    )
}