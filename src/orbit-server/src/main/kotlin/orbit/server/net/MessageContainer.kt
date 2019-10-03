/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.shared.mesh.Namespace
import orbit.shared.mesh.NodeId
import orbit.shared.net.Message

data class MessageMetadata(
    val connectedNamespace: Namespace,
    val connectedNode: NodeId,
    val messageDirection: MessageDirection,
    val respondOnError: Boolean = true
)

class MessageContainer(
    val message: Message,
    val metadata: MessageMetadata
)

enum class MessageDirection {
    INBOUND,
    OUTBOUND;
}